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


import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.entity.FormulaRateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.SemaphoreBasedRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Created by liuruisen on 2019/1/8.
 * // 1.TokenBucketRateLimiter support
 */
public class RateLimiterRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterRegistry.class);

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";


    private final Map<String, RateLimiter> rateLimiters;

    protected RateLimiterRegistry() {
        rateLimiters = new ConcurrentHashMap<>();
    }


    // get RateLimiter from registry
    protected RateLimiter getRateLimiter(String name) {
        return rateLimiters.get(name);
    }

    protected List<RateLimiter> getAllRateLimiters() {
        return rateLimiters.values().stream().collect(Collectors.toList());
    }


    protected RateLimiter rateLimiter(String name, RateLimiterConfig rateLimiterConfig, Integer limiterType) {
        requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        requireNonNull(rateLimiterConfig, CONFIG_MUST_NOT_BE_NULL);
        return rateLimiters.computeIfAbsent(
                name,
                limitName -> {
                    switch (limiterType) {
                        case 1: return new TokenBucketRateLimiter(name, rateLimiterConfig);
                        case 2: return new SemaphoreBasedRateLimiter(name, rateLimiterConfig);
                    }
                    return null;
                }
        );
    }

    protected void removeRateLimiter(String name, RateLimiter rateLimiter) {
        rateLimiters.remove(name, rateLimiter);
        logger.info("Remove RateLimiter successfully with name: {}", name);
    }


    protected RateLimiter addOrModRateLimiter(FormulaRateLimiterConfig ratelimiterConfig) {
        boolean isInitRatelimiter = true;
        RateLimiter resultRateLimiter = null;

        // RateLimiter name must not be null && RateLimiter threshold > 0 && enabled
        if (!StringUtils.isEmpty(ratelimiterConfig.getLimiterName())
                && ratelimiterConfig.getThreshold() != null && ratelimiterConfig.getEnabled() != null
                && ratelimiterConfig.getEnabled()) {

            RateLimiter rateLimiter = getRateLimiter(ratelimiterConfig.getLimiterName());
            if (rateLimiter != null) {
                // modify RateLimiter, thread safe
                rateLimiter.changeLimitForPeriod(ratelimiterConfig.getThreshold());
                rateLimiter.changeTimeoutDuration(
                        Duration.ofMillis(ratelimiterConfig.getTimeoutInMillis()));
                isInitRatelimiter = false;
                resultRateLimiter = rateLimiter;
                logger.info("Update RateLimiter successfully with name: {}, value {}",
                        ratelimiterConfig.getEffectiveLocation() + "#" + ratelimiterConfig.getHttpMethod(),
                        resultRateLimiter);
            }

            // new RateLimiter
            if (isInitRatelimiter) {
                resultRateLimiter = rateLimiter(ratelimiterConfig.getLimiterName(),
                        createRateLimiterConfig(ratelimiterConfig), ratelimiterConfig.getLimiterType());
                logger.info("Create RateLimiter successfully with name: {}, value {}",
                        ratelimiterConfig.getEffectiveLocation() + "#" + ratelimiterConfig.getHttpMethod(),
                        resultRateLimiter);
            }

            return resultRateLimiter;
        }

        return null;
    }


    private static RateLimiterConfig createRateLimiterConfig(FormulaRateLimiterConfig rateLimiterConfig) {
        if (rateLimiterConfig == null) {
            return null;
        }

        RateLimiterConfig.Builder rateLimiterConfigBuilder = RateLimiterConfig.custom();

        if (rateLimiterConfig.getThreshold() != null) {
            rateLimiterConfigBuilder.limitForPeriod(rateLimiterConfig.getThreshold());
        }

        // rateLimiterConfigBuilder.limitRefreshPeriod(Duration.ofMillis(1000)); // default 1s

        if (rateLimiterConfig.getTimeoutInMillis() != null) { // default 0
            rateLimiterConfigBuilder.timeoutDuration(Duration.ofMillis(rateLimiterConfig.getTimeoutInMillis()));
        }

        return rateLimiterConfigBuilder.build();
    }

}
