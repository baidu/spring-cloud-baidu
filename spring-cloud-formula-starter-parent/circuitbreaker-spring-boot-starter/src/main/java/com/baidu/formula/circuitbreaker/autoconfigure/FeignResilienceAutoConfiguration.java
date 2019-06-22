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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.baidu.formula.circuitbreaker.CircuitBreakerAutoConfiguration;
import com.baidu.formula.circuitbreaker.autoconfigure.feign.Resilience4jFeign;
import com.baidu.formula.circuitbreaker.impl.CircuitBreakerCore;

import feign.Feign;

/**
 * Created by cuiweizheng on 19/4/17.
 */
@ConditionalOnClass({Feign.class})
@Configuration
@AutoConfigureBefore(FeignClientsConfiguration.class)
@AutoConfigureAfter(CircuitBreakerAutoConfiguration.class)
@EnableConfigurationProperties({FeignHttpClientProperties.class})
// @Import({FeignRibbonClientAutoConfiguration.class})
public class FeignResilienceAutoConfiguration {

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public Feign.Builder feignBuilder(CircuitBreakerCore circuitBreakerCore) {
        return Resilience4jFeign.builder(circuitBreakerCore);
    }

}
