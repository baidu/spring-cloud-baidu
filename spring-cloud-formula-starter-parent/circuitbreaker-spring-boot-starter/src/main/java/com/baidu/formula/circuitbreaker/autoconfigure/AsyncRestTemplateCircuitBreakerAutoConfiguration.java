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
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.AsyncRestTemplateCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.web.client.AsyncRestTemplate;

import com.baidu.formula.circuitbreaker.impl.CircuitBreakerCore;

/**
 * Created by cuiweizheng on 19/4/20.
 */
@Configuration
@ConditionalOnBean(LoadBalancerClient.class)
@ConditionalOnClass(AsyncRestTemplate.class)
@AutoConfigureAfter({AsyncLoadBalancerAutoConfiguration.class})
public class AsyncRestTemplateCircuitBreakerAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(AsyncRestTemplateCircuitBreakerAutoConfiguration.class);

    @LoadBalanced
    @Autowired(required = false)
    private List<AsyncRestTemplate> restTemplates = Collections.emptyList();

    @Autowired(required = true)
    private CircuitBreakerCore circuitBreakerCore;

    @Bean
    public SmartInitializingSingleton loadBalancedAsyncRestTemplateInitializer2(
            final List<AsyncRestTemplateCustomizer> customizers) {
        return new SmartInitializingSingleton() {
            @Override
            public void afterSingletonsInstantiated() {
                logger.info("init AsyncRestTemplateCircuitBreaker start");
                for (AsyncRestTemplate restTemplate : AsyncRestTemplateCircuitBreakerAutoConfiguration.this
                        .restTemplates) {
                    AsyncRestTemplateCircuitInterceptor interceptor =
                            new AsyncRestTemplateCircuitInterceptor(circuitBreakerCore);
                    logger.info("add AsyncRestTemplateCircuitInterceptor first");
                    List<AsyncClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
                    interceptors.add(0, interceptor);
                    restTemplate.setInterceptors(interceptors);
                }
                logger.info("init AsyncRestTemplateCircuitBreaker end");
            }
        };
    }

}



