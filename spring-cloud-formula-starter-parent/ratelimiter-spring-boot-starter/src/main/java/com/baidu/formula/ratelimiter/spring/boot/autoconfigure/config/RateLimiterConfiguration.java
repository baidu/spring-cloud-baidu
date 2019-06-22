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

import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.RateLimiterEffectiveAspect;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.RateLimiterEffectiveFilter;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.RateLimiterGlobalEffectiveFilter;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.ratelimiter.RateLimiterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;


/**
 * Created by liuruisen on 2019/1/7.
 */
@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterConfiguration.class);


    @Bean
    // @ConditionalOnClass(RateLimiter.class)
    public RateLimiterManager ratelimiterRegistryManager(RateLimiterProperties rateLimiterProperties) {
        logger.debug("RateLimiterProperties at start are :{}", rateLimiterProperties);
        RateLimiterManager registryManager =
                new RateLimiterManager(rateLimiterProperties);

        logger.debug("Autoconfig RateLimiterManager bean success.");

        return registryManager;
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiterEffectiveAspect.class)
    @ConditionalOnClass(Controller.class)
    @ConditionalOnBean(RateLimiterManager.class)
    @ConditionalOnProperty(value = "formula.ratelimiter.aspect.enable", matchIfMissing = false)
    public RateLimiterEffectiveAspect ratelimiterEffectiveAspect(RateLimiterManager registryManager) {
        RateLimiterEffectiveAspect ratelimiterEffectiveAspect =
                new RateLimiterEffectiveAspect(registryManager);

        logger.debug("Autoconfig RateLimiterEffectiveAspect bean success.");

        return ratelimiterEffectiveAspect;
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiterEffectiveFilter.class)
    @ConditionalOnBean(RateLimiterManager.class)
    @Order(1000)
    public RateLimiterEffectiveFilter rateLimiterEffectiveFilter(RateLimiterManager registryManager) {
        RateLimiterEffectiveFilter rateLimiterEffectiveFilter =
                new RateLimiterEffectiveFilter(registryManager);

        logger.debug("Autoconfig RateLimiterEffectiveFilter bean success");

        return rateLimiterEffectiveFilter;
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiterGlobalEffectiveFilter.class)
    @ConditionalOnBean(RateLimiterManager.class)
    @Order(1001)
    public RateLimiterGlobalEffectiveFilter rateLimiterGlobalEffectiveFilter(
            RateLimiterManager registryManager) {
        RateLimiterGlobalEffectiveFilter rateLimiterGlobalEffectiveFilter = new RateLimiterGlobalEffectiveFilter(
                registryManager);

        logger.debug("Autoconfig RateLimiterGlobalEffectiveFilter bean success");

        return rateLimiterGlobalEffectiveFilter;
    }
}
