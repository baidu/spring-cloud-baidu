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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config;

import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.entity.FormulaRateLimiterConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by luoguangming on 2019/5/16.
 * Test for FormulaRateLimiterConfig
 */
public class FormulaRateLimiterConfigTest {

    @Test
    public void testSetAndGet() {
        FormulaRateLimiterConfig configuration = new FormulaRateLimiterConfig();
        configuration.setEnabled(true);
        assertEquals(true, configuration.getEnabled());

        String effectiveLocation = "test";
        configuration.setEffectiveLocation(effectiveLocation);
        assertEquals(effectiveLocation, configuration.getEffectiveLocation());

        Integer effectiveType = 1;
        configuration.setEffectiveType(effectiveType);
        assertEquals(effectiveType, configuration.getEffectiveType());

        String httpMethod = "GET";
        configuration.setHttpMethod(httpMethod);
        assertEquals(httpMethod, configuration.getHttpMethod());

        Long ruleId = 1L;
        configuration.setRuleId(ruleId);
        assertEquals(ruleId, configuration.getRuleId());

        Integer threshold = 1;
        configuration.setThreshold(threshold);
        assertEquals(threshold, configuration.getThreshold());

        Integer limiterType = 1;
        configuration.setLimiterType(limiterType);
        assertEquals(limiterType, configuration.getLimiterType());

        Integer timeoutInMillis = 1;
        configuration.setTimeoutInMillis(timeoutInMillis);
        assertEquals(timeoutInMillis, configuration.getTimeoutInMillis());

        String limiterName = effectiveLocation + "#" + httpMethod.toLowerCase();
        assertEquals(limiterName, configuration.getLimiterName());
        configuration.setEffectiveType(2);
        assertEquals(effectiveLocation, configuration.getLimiterName());
    }

}
