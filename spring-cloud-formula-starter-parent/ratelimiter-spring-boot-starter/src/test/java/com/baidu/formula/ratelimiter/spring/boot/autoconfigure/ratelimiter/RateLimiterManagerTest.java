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
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.RateLimiterProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by luoguangming on 2019/5/16.
 * Test for RateLimiterManager
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RateLimiterManagerTest {

    @Autowired
    RateLimiterManager registryManager;
    @Autowired
    RateLimiterProperties rateLimiterProperties;

    private Logger logger = LoggerFactory.getLogger(RateLimiterManagerTest.class);

    @Test
    public void testRateLimiterRuleUpdate() {

        List<FormulaRateLimiterConfig> configList = rateLimiterProperties.getRatelimiters();
        logger.info("Configure list: {}", configList);
        String key = configList.get(0).getLimiterName();
        Integer originalThreshHold = registryManager.getRatelimiterConfigs().get(key).getThreshold();
        Integer newThreshHold = originalThreshHold + 1;
        configList.get(0).setThreshold(newThreshHold);
        rateLimiterProperties.setRatelimiters(configList);
        // assert not equal before refresh
        assertNotEquals(newThreshHold, registryManager.getRatelimiterConfigs().get(key).getThreshold());

        // manually trigger change event
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("formula.ratelimiter.ratelimiters[0].threshold");
        EnvironmentChangeEvent changeEvent = new EnvironmentChangeEvent(changedKeys);
        registryManager.rateLimiterConfigRefresh(changeEvent);
        assertEquals(newThreshHold, registryManager.getRatelimiterConfigs().get(key).getThreshold());

        // change back
        configList.get(0).setThreshold(originalThreshHold);
        rateLimiterProperties.setRatelimiters(configList);
        registryManager.rateLimiterConfigRefresh(changeEvent);
        assertEquals(originalThreshHold, registryManager.getRatelimiterConfigs().get(key).getThreshold());
    }

    @Test
    public void testRateLimiterRuleDisable() {

        List<FormulaRateLimiterConfig> configList = rateLimiterProperties.getRatelimiters();
        logger.info("Configure list: {}", configList);
        String key = configList.get(1).getLimiterName();
        configList.get(1).setEnabled(false);
        rateLimiterProperties.setRatelimiters(configList);
        // assert not equal before refresh
        assertNotEquals(false, registryManager.getRatelimiterConfigs().get(key).getEnabled());

        // manually trigger change event
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("formula.ratelimiter.ratelimiters[1].enabled");
        EnvironmentChangeEvent changeEvent = new EnvironmentChangeEvent(changedKeys);
        registryManager.rateLimiterConfigRefresh(changeEvent);
        assertEquals(false, registryManager.getRatelimiterConfigs().get(key).getEnabled());

        // change back
        configList.get(1).setEnabled(true);
        rateLimiterProperties.setRatelimiters(configList);
        registryManager.rateLimiterConfigRefresh(changeEvent);
        assertEquals(true, registryManager.getRatelimiterConfigs().get(key).getEnabled());
    }

    @Test
    public void testRateLimiterRuleDelete() {

        List<FormulaRateLimiterConfig> configList = rateLimiterProperties.getRatelimiters();
        logger.info("Configure list: {}", configList);
        FormulaRateLimiterConfig configuration = configList.get(1);
        configList.remove(1);
        assertEquals(2, registryManager.getRatelimiterConfigs().size());

        // manually trigger change event
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("formula.ratelimiter.ratelimiters[1]");
        EnvironmentChangeEvent changeEvent = new EnvironmentChangeEvent(changedKeys);
        registryManager.rateLimiterConfigRefresh(changeEvent);
        assertEquals(1, registryManager.getRatelimiterConfigs().size());

        // change back
        configList.add(configuration);
        registryManager.rateLimiterConfigRefresh(changeEvent);
        assertEquals(2, registryManager.getRatelimiterConfigs().size());
    }
}
