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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Created by luoguangming on 2019/5/23.
 * Configuration for RestTemplateRateLimiterInterceptor
 */
@Configuration
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnBean(LoadBalancerClient.class)
@EnableConfigurationProperties(LoadBalancerRetryProperties.class)
@AutoConfigureAfter({LoadBalancerAutoConfiguration.class, AsyncLoadBalancerAutoConfiguration.class})
public class RestTemplateRateLimiterAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateRateLimiterAutoConfiguration.class);

    @Value("${spring.application.name}")
    private String serviceName;

    @LoadBalanced
    @Autowired(required = false)
    private List<RestTemplate> restTemplates = Collections.emptyList();

    @Bean
    public SmartInitializingSingleton loadBalancedRestTemplateInitializer3(
            final ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {
        return () -> {
            logger.info("Init RestTemplateRateLimiterAutoConfiguration");
            for (RestTemplate restTemplate : RestTemplateRateLimiterAutoConfiguration.this.restTemplates) {
                List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
                logger.debug("RestTemplate init start, interceptor size:" + interceptors.size());
                ClientHttpRequestInterceptor interceptor = new RestTemplateRateLimiterInterceptor(serviceName);
                interceptors.add(0, interceptor);
                restTemplate.setInterceptors(interceptors);
                logger.debug("RestTemplate init end, add RestTemplateRateLimiterInterceptor");
            }
        };
    }
}
