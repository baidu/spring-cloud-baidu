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

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.github.resilience4j.ratelimiter.internal.RateLimiterEventProcessor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by liuruisen on 2019/1/8.
 * use Guava Ratelimiter - SmoothBursty
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String CONFIG_MUST_NOT_BE_NULL = "RateLimiterConfig must not be null";

    private final String name;

    private final AtomicInteger waitingThreads;

    private final com.google.common.util.concurrent.RateLimiter rateLimiter;

    private final AtomicReference<RateLimiterConfig> rateLimiterConfig;

    private final RateLimiterEventProcessor eventProcessor;

    private final TokenBucketRateLimiterMetrics metrics;

    public TokenBucketRateLimiter(String name, RateLimiterConfig rateLimiterConfig) {
        waitingThreads = new AtomicInteger(0);
        this.name = requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        this.rateLimiterConfig = new AtomicReference<>(requireNonNull(rateLimiterConfig, CONFIG_MUST_NOT_BE_NULL));
        rateLimiter = com.google.common.util.concurrent.RateLimiter.create(rateLimiterConfig.getLimitForPeriod());
        eventProcessor = new RateLimiterEventProcessor();
        metrics = new TokenBucketRateLimiterMetrics();
    }


    @Override
    public void changeTimeoutDuration(Duration timeoutDuration) {
        RateLimiterConfig newConfig = RateLimiterConfig.from(rateLimiterConfig.get())
                .timeoutDuration(timeoutDuration)
                .build();
        rateLimiterConfig.set(newConfig);

    }

    @Override
    public void changeLimitForPeriod(int limitForPeriod) {
        // set config
        RateLimiterConfig newConfig = RateLimiterConfig.from(rateLimiterConfig.get())
                .limitForPeriod(limitForPeriod)
                .build();
        rateLimiterConfig.set(newConfig);
        // set Guava RateLimiter rate()
        rateLimiter.setRate(limitForPeriod);
    }

    @Override
    public boolean getPermission(Duration timeoutDuration) {
        boolean success = rateLimiter.tryAcquire(1, timeoutDuration.toMillis(), MILLISECONDS);
        publishRateLimiterEvent(success);
        return success;
    }

    @Override
    public long reservePermission(Duration timeoutDuration) {
        return -1;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return rateLimiterConfig.get();
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return null;
    }

    private final class TokenBucketRateLimiterMetrics implements Metrics {
        private TokenBucketRateLimiterMetrics() {
        }

        @Override
        public int getAvailablePermissions() {
            return 0;
        }

        @Override
        public int getNumberOfWaitingThreads() {
            return waitingThreads.get();
        }
    }


    private void publishRateLimiterEvent(boolean permissionAcquired) {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        if (permissionAcquired) {
            eventProcessor.consumeEvent(new RateLimiterOnSuccessEvent(name));
            return;
        }
        eventProcessor.consumeEvent(new RateLimiterOnFailureEvent(name));
    }
}
