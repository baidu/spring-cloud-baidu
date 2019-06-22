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
package com.baidu.formula.test.timelimiter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class Resilience4jTest {
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jTest.class);

    private TimeLimiter timeLimiter;

    private ExecutorService executorService;

    private CircuitBreaker circuitBreaker;

    @Before
    public void init() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .cancelRunningFuture(true)
                .build();
         timeLimiter = TimeLimiter.of(timeLimiterConfig);

        CustomizableThreadFactory factory = new CustomizableThreadFactory("timeLimiter-");
        factory.setDaemon(true);
        executorService = Executors.newCachedThreadPool(factory);

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig
                .custom()
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(10)
                .ringBufferSizeInHalfOpenState(2)
                .build();

        circuitBreaker = CircuitBreaker.of("backendName", circuitBreakerConfig);
    }


    @Test
    public void testTimeout() throws Throwable {
        Callable<Object> callable = () -> {
            Thread.sleep(10000);
            return null;
        };
        Supplier<Future<Object>> supplier = () -> executorService.submit(callable);
        // Wrap your call to BackendService.doSomething() in a future provided by your executor
        Callable<Object> result = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);
        long start = System.currentTimeMillis();
        try {
            result.call();
            fail("fail");
        } catch (Throwable t) {
            long cost = System.currentTimeMillis() - start;
            assertThat(cost, Matchers.lessThan(1010L));
            assertThat(t, Matchers.instanceOf(TimeoutException.class));
        }
    }

    @Test
    public void testCircuitBreakerWithOpen() {
        Supplier<Void> supplier = () -> {
            this.demoLogic(80);
            return null;
        };
        for (int i = 0; i < 1000; i++) {
            try {
                Void result = circuitBreaker.executeSupplier(supplier);
                System.out.println(result);
            } catch (Exception e) {
                // do nothing
            }
        }

        CircuitBreaker.State state = circuitBreaker.getState();
        assertEquals(state, CircuitBreaker.State.OPEN);
    }

    public void demoLogic(int rate) {
        int val = random.nextInt(100);
        if (val < rate) {
            throw new RuntimeException();
        }
    }

    Random random = new Random();
}
