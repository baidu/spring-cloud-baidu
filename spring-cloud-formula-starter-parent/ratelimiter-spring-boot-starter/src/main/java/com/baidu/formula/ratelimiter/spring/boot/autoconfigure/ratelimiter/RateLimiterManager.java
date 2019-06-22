/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.ratelimiter;

import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.RateLimiterProperties;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.entity.FormulaRateLimiterConfig;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.exception.BlockException;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.util.FormulaConfigUtils;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by liuruisen on 2019/1/8.
 * Modified by luoguangming
 */
public class RateLimiterManager {

    private Logger logger = LoggerFactory.getLogger(RateLimiterManager.class);

    private RateLimiterRegistry rateLimiterRegistry;

    private RateLimiterProperties rateLimiterProperties;

    private Map<String, FormulaRateLimiterConfig> ratelimiterConfigs;

    // store PatternRequestConditions, map for concurrent
    private Map<String, PatternsRequestCondition> patternsRequestMap;

    public RateLimiterManager(RateLimiterRegistry rateLimiterRegistry,
                              RateLimiterProperties rateLimiterProperties) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.rateLimiterProperties = rateLimiterProperties;
        updateRateLimiterConfigMap();
        updatePatternsRequestMap();
        initRateLimiters();
    }

    public RateLimiterManager(RateLimiterProperties rateLimiterProperties) {
        this(new RateLimiterRegistry(), rateLimiterProperties);
    }

    public RateLimiter getRateLimiterFromRegistry(String name) {
        try {
            Assert.notNull(rateLimiterRegistry, "RateLimiterRegistry is null");
            Assert.notNull(rateLimiterRegistry.getAllRateLimiters(), "Registry RateLimiters is null");
        } catch (Exception e) {
            logger.debug("There are some errors in RateLimiterRegistry {}", e.getMessage());
            return null;
        }
        return rateLimiterRegistry.getRateLimiter(name);

    }

    public void initRateLimiters(){
        for(String name: ratelimiterConfigs.keySet()){
            refreshRateLimiter(name, 1);
        }
    }

    // effectiveType  1 : uri  2 : method
    public void refreshRateLimiter(String name, Integer effectiveType) {
        // when exception appear, fast fail
        try {
            Assert.notNull(ratelimiterConfigs, "RateLimiterProperties is null");
            Assert.isTrue(ratelimiterConfigs.size() > 0, "Properties RateLimiters is null");
            Assert.notEmpty(Arrays.asList(name), "RateLimiter name is empty");
        } catch (Exception e) {
            logger.debug("There are some errors in RateLimiterProperties {}", e.getMessage());
            return;
        }

        // refresh the rate limiter for the uri
        FormulaRateLimiterConfig limiterConfig = ratelimiterConfigs.get(name);
        RateLimiter rateLimiter = getRateLimiterFromRegistry(name);

        if (limiterConfig != null) {
            if (limiterConfig.getEffectiveType().equals(effectiveType) && limiterConfig.getEnabled()) {
                // ADD or update RateLimiter
                rateLimiterRegistry.addOrModRateLimiter(limiterConfig);

            } else if (limiterConfig.getEffectiveType().equals(effectiveType) && !limiterConfig.getEnabled()
                    && rateLimiter != null) {
                // disable
                rateLimiterRegistry.removeRateLimiter(name, rateLimiter);
            }
        } else {
            // delete
            if (rateLimiter != null) {
                rateLimiterRegistry.removeRateLimiter(name, rateLimiter);
            }
        }
    }

    public void waitForPermit(RateLimiter rateLimiter) {
        if (rateLimiter != null) {
            try {
                // wait for permission, fast fail
                RateLimiter.waitForPermission(rateLimiter);
            } catch (IllegalStateException e) {
                throw e;
            } catch (RequestNotPermitted requestNotPermitted) {
                // convert exception
                throw new BlockException("The request has been block, please try later");
            }
        }
    }

    @Order
    @EventListener
    public void rateLimiterConfigRefresh(EnvironmentChangeEvent changeEvent) {
        Set<String> refreshKey = changeEvent.getKeys();
        logger.debug("Received configuration update with keys: {}", refreshKey);
        logger.debug("Updated rateLimiterProperties are :{}", rateLimiterProperties);
        if (refreshKey != null && refreshKey.size() > 0
                && refreshKey.toString().contains("formula.ratelimiter")) {
            Set<FormulaRateLimiterConfig> cachedRateLimiterConfigs = new HashSet<>(ratelimiterConfigs.values());
            List<FormulaRateLimiterConfig> newRateLimiterConfigs = rateLimiterProperties.getRatelimiters();
            Set<String> names = getNamesWithUpdates(cachedRateLimiterConfigs, newRateLimiterConfigs);
            updateRateLimiterConfigMap();
            updatePatternsRequestMap();
            if (null != names && names.size() > 0) {
                for (String name : names) {
                    logger.debug("RateLimiter to be refresh with name: {}", name);
                    refreshRateLimiter(name, 1);
                }
            }
        }
    }

    // get names with updates (add, modify, delete)
    private Set<String> getNamesWithUpdates(Set<FormulaRateLimiterConfig> cachedRateLimiterConfigs, List<FormulaRateLimiterConfig> newRateLimiterConfigs) {
        Set<FormulaRateLimiterConfig> configs = new HashSet<>(cachedRateLimiterConfigs);
        if (null != newRateLimiterConfigs && newRateLimiterConfigs.size() > 0) {
            for (FormulaRateLimiterConfig configuration : newRateLimiterConfigs) {
                if (configs.contains(configuration)) {
                    configs.remove(configuration);
                } else {
                    configs.add(configuration);
                }
            }
        }
        Set<String> names = new HashSet<>();
        if (null != configs && configs.size() > 0) {
            for (FormulaRateLimiterConfig configuration : configs) {
                String name = configuration.getEffectiveLocation() + "#" + configuration.getHttpMethod().toLowerCase();
                names.add(name);
            }
        }
        return names;
    }

    // init or modify configMap
    private void updateRateLimiterConfigMap() {

        if (rateLimiterProperties == null
                || rateLimiterProperties.getRatelimiters() == null
                || rateLimiterProperties.getRatelimiters().size() <= 0) {
            setRateLimiterConfigs(new ConcurrentHashMap<>());
        } else {
            Map<String, FormulaRateLimiterConfig> limiterMap =
                    rateLimiterProperties.getRatelimiters().stream()
                            .filter(config -> FormulaConfigUtils.isRateLimiterRuleMatch(config))
                            .collect(Collectors.toMap(FormulaRateLimiterConfig::getLimiterName,
                            config -> (FormulaRateLimiterConfig) config.clone(), (k1, k2) -> k1));

            setRateLimiterConfigs(limiterMap);
            logger.info("Update RateLimiter config map success, size: " + limiterMap.size());
        }
    }

    // init or modify patternsRequestMap
    private void updatePatternsRequestMap() {
        if (rateLimiterProperties == null
                || rateLimiterProperties.getRatelimiters() == null
                || rateLimiterProperties.getRatelimiters().size() <= 0) {
            setPatternsRequestMap(new ConcurrentHashMap<>());
        } else {
            Map<String, PatternsRequestCondition> patternsMap = new ConcurrentHashMap<>();
            rateLimiterProperties.getRatelimiters().forEach(limiterConfig -> {
                if (FormulaConfigUtils.isRateLimiterRuleMatch(limiterConfig) && limiterConfig.getEnabled()) {
                    patternsMap.putIfAbsent(limiterConfig.getEffectiveLocation(),
                            new PatternsRequestCondition(limiterConfig.getEffectiveLocation()));
                }
            });
            setPatternsRequestMap(patternsMap);
            logger.info("Update RateLimiter rule pattern success, size: " + patternsMap.size());
        }
    }

    public Map<String, FormulaRateLimiterConfig> getRatelimiterConfigs() {
        return ratelimiterConfigs;
    }

    public void setRateLimiterConfigs(Map<String, FormulaRateLimiterConfig> rateLimiterConfigs) {
        this.ratelimiterConfigs = rateLimiterConfigs;
    }

    public Map<String, PatternsRequestCondition> getPatternsRequestMap() {
        return patternsRequestMap;
    }

    public void setPatternsRequestMap(Map<String, PatternsRequestCondition> patternsRequestMap) {
        this.patternsRequestMap = patternsRequestMap;
    }
}
