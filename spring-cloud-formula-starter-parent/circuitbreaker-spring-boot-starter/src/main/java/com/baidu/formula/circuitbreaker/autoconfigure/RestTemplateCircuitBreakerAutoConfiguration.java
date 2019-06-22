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
/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.formula.circuitbreaker.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.baidu.formula.circuitbreaker.impl.CircuitBreakerCore;

/**
 * Created by cuiweizheng on 19/4/18.
 */
@Configuration
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnBean(LoadBalancerClient.class)
@EnableConfigurationProperties(LoadBalancerRetryProperties.class)
@AutoConfigureAfter({LoadBalancerAutoConfiguration.class,
        AsyncLoadBalancerAutoConfiguration.class})
public class RestTemplateCircuitBreakerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateCircuitBreakerAutoConfiguration.class);

    @LoadBalanced
    @Autowired(required = false)
    private List<RestTemplate> restTemplates = Collections.emptyList();

    @Autowired(required = true)
    private CircuitBreakerCore circuitBreakerCore;

    @Bean
    public SmartInitializingSingleton loadBalancedRestTemplateInitializer2(
            final ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {
        return () -> {
            logger.info("RestTemplateResilienceAutoConfiguration init2");
            for (RestTemplate restTemplate : RestTemplateCircuitBreakerAutoConfiguration.this.restTemplates) {
                List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
                logger.info("RestTemplate init2 start,interceptor size:" + interceptors.size());
                //for (ClientHttpRequestInterceptor interceptor : interceptors) {
                //logger.info("RestTemplate init2 interceptor ing:"+interceptor.getClass().getCanonicalName());
                //}
                //logger.info("RestTemplate init2 Customizer end");
                ClientHttpRequestInterceptor interceptor1 = new RestTemplateCircuitBreakerInterceptor
                        (circuitBreakerCore);
                interceptors.add(0, interceptor1);
                restTemplate.setInterceptors(interceptors);
                logger.info("RestTemplate init2 end,add CircuitBreaker interceptor");
            }
        };
    }
}
