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

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Created by luoguangming on 2019/5/6.
 * Test for TokenBucketRateLimiter
 */
public class TokenBucketRateLimiterTest {

    private RateLimiterConfig rateLimiterConfig;
    private int limitsForPeriod;

    @Before
    public void init() {
        limitsForPeriod = 2;
        RateLimiterConfig.Builder rateLimiterConfigBuilder = RateLimiterConfig.custom();
        rateLimiterConfigBuilder.limitForPeriod(limitsForPeriod);
        rateLimiterConfig = rateLimiterConfigBuilder.build();
    }

    @Test
    public void testBasic() {
        String name = "rateLimiter";
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(name, rateLimiterConfig);
        assertEquals(name, limiter.getName());
        assertEquals(limitsForPeriod, limiter.getRateLimiterConfig().getLimitForPeriod());
        assertEquals(null, limiter.getEventPublisher());
        assertNotEquals(null, limiter.getMetrics());
    }

    @Test
    public void testBlockingModel() {
        Duration duration = Duration.ofSeconds(10, 0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter("blocking-rateLimiter", rateLimiterConfig);
        boolean success;
        success = simulateRequest(duration, limiter, 0); // timeCost=0, since it's the first request
        assertTrue(success);
        success = simulateRequest(duration, limiter, 500);
        assertTrue(success);
        success = simulateRequest(duration, limiter, 500);
        assertTrue(success);
    }

    @Test
    public void testUnBlockingModel() throws InterruptedException {
        Duration duration = Duration.ofSeconds(0, 0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter("unblocking-rateLimiter", rateLimiterConfig);
        boolean success;
        success = simulateRequest(duration, limiter, 0);
        assertTrue(success);
        success = simulateRequest(duration, limiter, 0);
        assertFalse(success);
        success = simulateRequest(duration, limiter, 0);
        assertFalse(success);
        Thread.sleep(500);
        success = simulateRequest(duration, limiter, 0);
        assertTrue(success);
    }

    private boolean simulateRequest(Duration duration, TokenBucketRateLimiter limiter, int expectTimeCostInMillis) {
        long oldTime = Calendar.getInstance().getTimeInMillis();
        //logger.info("old : {}", oldTime);
        boolean success = limiter.getPermission(duration);
        long newTime = Calendar.getInstance().getTimeInMillis();
        //logger.info("new : {}, success :{}", newTime, success);
        assertEquals(expectTimeCostInMillis, newTime - oldTime, 20);
        return success;
    }

}
