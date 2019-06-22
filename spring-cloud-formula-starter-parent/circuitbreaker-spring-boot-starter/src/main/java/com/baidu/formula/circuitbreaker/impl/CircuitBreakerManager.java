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
package com.baidu.formula.circuitbreaker.impl;

import com.baidu.formula.circuitbreaker.config.CircuitBreakerProperties;
import com.baidu.formula.circuitbreaker.model.CircuitBreakerCoalition;
import com.baidu.formula.circuitbreaker.model.CircuitBreakerRule;
import com.baidu.formula.circuitbreaker.model.TimeLimiterCoalition;
import com.baidu.formula.engine.tag.FormulaSource;
import com.baidu.formula.engine.tag.FormulaTag;
import com.baidu.formula.engine.tag.Operation;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Initialization and update thr circuitbreaker rules;
 * Get the best matching rule based on the request parameters
 *
 * @author Bowu Dong (tq02ksu@gmail.com)
 * modified by guoblin
 */
public class CircuitBreakerManager {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerManager.class);

    private static final String PREFIX = "formula.circuitBreaker";

    private static Integer HTTP = 1;

    private static Integer RPC = 2;

    private static Integer MENTHOD = 3;

    private static String ALL_SERVICE_NAME = "*ALL_SERVICE_NAME*";

    private static String ALL_PATTERN = "*ALL_WAY*";

    private static String ALL_LOCATION = "*ALL*";

    private final CircuitBreakerProperties properties;

    private ConcurrentHashMap<String, TimeLimiterCoalition> timeLimiterMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, CircuitBreakerCoalition> circuitBreakerMap = new ConcurrentHashMap<>();

    public CircuitBreakerManager(CircuitBreakerProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        for (CircuitBreakerRule rule : properties.getRules()) {
            String ruleName = getRuleName(rule);
            if (ruleName != null) {
                CircuitBreakerCoalition circuitBreakerCoalition = createCircuitBreaker(ruleName, rule);
                if (circuitBreakerCoalition != null &&
                        circuitBreakerCoalition.getCircuitBreaker() != null) {
                    circuitBreakerMap.putIfAbsent(ruleName, circuitBreakerCoalition);
                }
                TimeLimiterCoalition timeLimiterCoalition = createTimeLimiter(ruleName, rule);
                if (timeLimiterCoalition != null &&
                        timeLimiterCoalition.getTimeLimiter() != null) {
                    timeLimiterMap.putIfAbsent(ruleName, timeLimiterCoalition);
                }
            }
        }
    }

    private CircuitBreakerCoalition createCircuitBreaker(String ruleName, CircuitBreakerRule rule) {
        if (rule == null) {
            return null;
        }
        if (rule.getEnabled() != null && rule.getEnabled() && rule.getFailureRateThreshold() != null) {
            try {
                CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                        .failureRateThreshold(rule.getFailureRateThreshold())
                        .waitDurationInOpenState(rule.getWaitDurationInOpenState())
                        .ringBufferSizeInHalfOpenState(rule.getRingBufferSizeInHalfOpenState())
                        .ringBufferSizeInClosedState(rule.getRingBufferSizeInClosedState())
                        .build();

                CircuitBreaker circuitBreaker = CircuitBreaker.of(ruleName, circuitBreakerConfig);
                if (rule.getForceOpen() != null && rule.getForceOpen()) {
                    circuitBreaker.transitionToForcedOpenState();
                }
                CircuitBreakerCoalition circuitBreakerCoalition = new CircuitBreakerCoalition(
                        circuitBreaker, rule);
                return circuitBreakerCoalition;
            } catch (Throwable e) {
                logger.error("failed to create circuitBreaker,name:{},id{}",
                        ruleName, rule.getRuleId(), e);
                return null;
            }
        }
        return null;
    }

    private TimeLimiterCoalition createTimeLimiter(String ruleName, CircuitBreakerRule rule) {
        if (rule == null) {
            return null;
        }
        Duration timeout = rule.getTimeoutDuration();
        if (rule.getEnabled() != null && rule.getEnabled() &&
                timeout != null && !timeout.isNegative() && !timeout.isZero()) {
            try {
                TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                        .timeoutDuration(rule.getTimeoutDuration())
                        .cancelRunningFuture(rule.getCancelRunningFuture())
                        .build();
                TimeLimiter timeLimiter = TimeLimiter.of(timeLimiterConfig);
                TimeLimiterCoalition timeLimiterCoalition = new TimeLimiterCoalition(
                        timeLimiter, rule);
                return timeLimiterCoalition;
            } catch (Exception e) {
                logger.error("failed to create timeLimiter,name:{},id{}",
                        ruleName, rule.getRuleId(), e);
                return null;
            }
        }
        return null;
    }

    /**
     * Rule name must be unique
     *
     * @param rule
     * @return http/rpc熔断：serviceName+httpMenthod+url
     * menthd熔断：menthod
     */
    private String getRuleName(CircuitBreakerRule rule) {
        if (rule == null) {
            return null;
        } else if (rule.getEffectiveType() == MENTHOD) {
            if (effectiveInfo(rule.getMethod())) {
                return rule.getRuleName();
            }
        } else {
            if (effectiveInfo(rule.getServiceName()) && effectiveInfo(rule.getEffectivePattren()) &&
                    effectiveInfo(rule.getEffectiveLocation())) {
                return rule.getRuleName();
            }
        }
        return null;
    }

    private static Boolean effectiveInfo(String info) {
        if (info != null && info.trim().length() > 0) {
            return true;
        }
        return false;
    }

    /**
     * 刷新时更改配置
     * CircuitBreaker和CircuitBreakerConfig都暂未提供修改配置的功能.
     * 配置项修改后,只能新创建一个CircuitBreaker替换原来的.
     * 带来的影响是原CircuitBreaker的数据都消失了.比如原来已经打开了,修改后都要重新计算.
     * 需要根据修改,精确刷新
     *
     * @param changeEvent
     */
    @Order
    @EventListener
    public void circuitBreakerConfigRefresh(EnvironmentChangeEvent changeEvent) {
        Set<String> refreshKeys = changeEvent.getKeys();
        if (CollectionUtils.isEmpty(refreshKeys)) {
            return;
        }

        if (refreshKeys != null && refreshKeys.size() > 0
                && refreshKeys.toString().contains(PREFIX)) {
            List<CircuitBreakerRule> rules = properties.getRules();
            if (rules == null) {
                rules = new ArrayList<>();
            }

            // delete nonexistent rule
            Set<String> newNames = rules.stream().map(rule -> getRuleName(rule)).collect(Collectors.toSet());
            for (Map.Entry<String, TimeLimiterCoalition> entry : timeLimiterMap.entrySet()) {
                if (!newNames.contains(entry.getKey())) {
                    logger.info("remove timeLimiterRule--ruleName:{}", entry.getKey());
                    timeLimiterMap.remove(entry.getKey());
                }
            }
            for (Map.Entry<String, CircuitBreakerCoalition> entry : circuitBreakerMap.entrySet()) {
                if (!newNames.contains(entry.getKey())) {
                    logger.info("remove circuitBreakerRule--ruleName:{}", entry.getKey());
                    circuitBreakerMap.remove(entry.getKey());
                }
            }

            for (CircuitBreakerRule rule : rules) {
                // 维持不变的  添加更改
                refreshRule(rule);
            }
        }
    }

    /**
     * Remove nonexistent rule first;
     * If the rules have not changed, keep it,
     * otherwise replace it with the new one.
     *
     * @param rule
     */
    private void refreshRule(CircuitBreakerRule rule) {
        String ruleName = getRuleName(rule);
        if (ruleName == null) {
            return;
        }
        try {
            CircuitBreakerCoalition coalition = circuitBreakerMap.get(ruleName);
            CircuitBreakerRule existRule = coalition == null ? null : coalition.getRule();
            refreshCircuitBreaker(ruleName, rule, existRule);
            refreshTimeLimiter(ruleName, rule, existRule);
        } catch (Exception e) {
            logger.info("refresh rule:ruleId:{}, ruleName:{} fail with exception", rule.getRuleId(), ruleName, e);
        }
    }

    private void refreshCircuitBreaker(String name, CircuitBreakerRule rule, CircuitBreakerRule existRule) {
        if (rule == null && existRule != null) {
            logger.info("remove  circuitBreakerRule,id:{}, name:{}", existRule.getRuleId(), name);
            circuitBreakerMap.remove(name);
        } else if (rule != null && existRule == null) {
            CircuitBreakerCoalition coalitionrNew = createCircuitBreaker(name, rule);
            if (coalitionrNew != null) {
                logger.info("add new circuitBreakerRule, id:{}, name:{}", rule.getRuleId(), name);
                circuitBreakerMap.put(name, coalitionrNew);
            }
        } else if (rule != null && existRule != null) {
            if (isCircuitBreakerConfigChanged(rule, existRule)) {
                // update rule ,replace it with the new one.
                circuitBreakerMap.remove(name);
                CircuitBreakerCoalition coalitionNew = createCircuitBreaker(name, rule);
                if (coalitionNew != null) {
                    logger.info("update circuitBreaker id:{}, name:{}", rule.getRuleId(), name);
                    circuitBreakerMap.put(name, coalitionNew);
                }
            }
        } else if (rule == null && existRule == null) {
            logger.info("no rule and  circuitBreaker name:{}", name);
        }
    }

    /**
     * Compare the configuration of rule with the exist one;
     *
     * @param newRule
     * @param existRule
     * @return
     */
    private boolean isCircuitBreakerConfigChanged(CircuitBreakerRule newRule,
                                                  CircuitBreakerRule existRule) {
        FormulaSource newSource = newRule.getSource();
        FormulaSource exitSource = existRule.getSource();

        if (isSourceChange(newSource, exitSource)) {
            return true;
        }
        Boolean state = existRule.getForceOpen();

        if (newRule.getEnabled() != existRule.getEnabled()) {
            return true;
        }

        if (newRule.getForceOpen() != null && newRule.getForceOpen()) {
            if (state != null && newRule.getForceOpen().equals(state)) {
                return false;
            } else {
                return true;
            }
        } else {
            if (state != null && CircuitBreaker.State.FORCED_OPEN.equals(state)) {
                return true;
            }
        }
        // 比较熔断器配置
        if (!newRule.getFailureRateThreshold().equals(existRule.getFailureRateThreshold())) {
            return true;
        }
        if (!newRule.getWaitDurationInOpenState().equals(existRule.getWaitDurationInOpenState())) {
            return true;
        }
        if (!newRule.getRingBufferSizeInClosedState().equals(existRule.getRingBufferSizeInClosedState())) {
            return true;
        }
        if (!newRule.getRingBufferSizeInHalfOpenState().equals(existRule.getRingBufferSizeInHalfOpenState())) {
            return true;
        }
        return false;
    }

    private boolean isSourceChange(FormulaSource newSource, FormulaSource exitSource) {
        try {
            if (newSource == null || exitSource == null) {
                if (newSource == exitSource) {
                    return false;
                }
                return true;
            }
            if (newSource.getTags().size() != exitSource.getTags().size()) {
                return true;
            }
            if (!newSource.equals(exitSource)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.info("SourceChange judge fail ", e);
        }
        return true;
    }

    private void refreshTimeLimiter(String name, CircuitBreakerRule rule, CircuitBreakerRule existRule) {
        if (rule == null && existRule != null) {
            logger.info("clear  timeLimiter id :{}, name:{}", existRule.getRuleId(), name);
            timeLimiterMap.remove(name);
        } else if (rule != null && existRule == null) {
            // add
            TimeLimiterCoalition coalitionNew = createTimeLimiter(name, rule);
            if (coalitionNew != null) {
                logger.info("add  timeLimiter id:{}, name:{}", rule.getRuleId(), name);
                timeLimiterMap.put(name, coalitionNew);
            }
        } else if (rule != null && existRule != null) {
            if (isTimeLimiterConfigChanged(rule, existRule)) {
                timeLimiterMap.remove(name);
                TimeLimiterCoalition coalitionNew = createTimeLimiter(name, rule);
                if (coalitionNew != null) {
                    logger.info("update timeLimiter id:{}, name:{}", rule.getRuleId(), name);
                    timeLimiterMap.put(name, coalitionNew);
                }
            }
        } else if (rule == null && existRule == null) {
            logger.info("no rule and  timeLimiter name:{}", name);
        }
    }

    /**
     *
     * TimeLimiterConfigs changes are not support now;
     *
     * @param rule
     * @param existRule
     * @return
     */
    private boolean isTimeLimiterConfigChanged(CircuitBreakerRule rule,
                                               CircuitBreakerRule existRule) {
        return true;
    }

    /**
     * Return the closest matching rule,
     * if no matching rules, return null
     * Higher precision rules with higher matching priority
     *
     * @param httpMethod
     * @param serviceName
     * @param url
     * @return
     */
    public CircuitBreakerCoalition getCircuitBreakerCoalition(String httpMethod,
                                                              String serviceName, String url) {
        CircuitBreakerCoalition tagCoalition = null;
        List<String> targNames = new ArrayList<>();
        targNames.add(serviceName + httpMethod + url);
        targNames.add(serviceName + httpMethod + ALL_LOCATION);
        targNames.add(serviceName + ALL_PATTERN + ALL_LOCATION);
        targNames.add(ALL_SERVICE_NAME + ALL_PATTERN + ALL_LOCATION);

        for (String targetName : targNames) {
            tagCoalition = getCircuitBreakerCoalition(targetName);
            if (tagCoalition != null) {
                break;
            }
        }
        return tagCoalition;
    }

    /**
     * Obtain the rule corresponding to the name
     * and determine whether the instance meets the rule requirements.
     *
     * @param targName
     * @return
     */
    public CircuitBreakerCoalition getCircuitBreakerCoalition(String targName) {
        CircuitBreakerCoalition targCoaliton = circuitBreakerMap.get(targName);
        if (targCoaliton != null && targCoaliton.getRule() != null) {
            if (matchRule(targCoaliton.getRule())) {
                return targCoaliton;
            }
        }
        return null;
    }

    /**
     * Determine whether the current instance
     * satisfies the source attribute requirement of the rule
     * if the source is null ,return true;
     * otherwise hit all rules return true
     *
     * @param rule
     * @return
     */
    private boolean matchRule(CircuitBreakerRule rule) {
        if (rule == null) {
            return false;
        }
        if (rule.getSource() == null ||
                rule.getSource().getTags() == null) {
            return true;
        }
        List<FormulaTag> formulaTags = rule.getSource().getTags();
        for (FormulaTag tag : formulaTags) {
            if (!Operation.isOperationMatch(tag.getOp(),
                    tag.getValue(), System.getenv(tag.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * timiter Waiting for subsequent support
     *googlefa
     * @param httpMethod
     * @param serviceName
     * @param url
     * @return
     */
    public TimeLimiterCoalition getTimeLimiterCoalition(String httpMethod,
                                                        String serviceName, String url) {
        return null;
    }
}
