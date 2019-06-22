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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.interceptor;

import feign.Feign;
import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by luoguangming on 2019/5/23.
 * Configuration for FeignRequestInterceptor
 */
@ConditionalOnClass({Feign.class})
@Configuration
@AutoConfigureBefore(FeignClientsConfiguration.class)
@EnableConfigurationProperties({FeignHttpClientProperties.class})
public class FeignRequestConfiguration {

    @Value("${spring.application.name}")
    private String serviceName;

    private static final Logger logger =
            LoggerFactory.getLogger(AsyncRestTemplateRateLimiterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor feignRequestInterceptor() {
        logger.info("Init RequestInterceptor Bean");
        return new FeignRequestInterceptor(serviceName);
    }
}
