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
package com.baidubce.formula.consul.config.spring.boot;

import com.baidubce.formula.consul.config.spring.boot.auth.BmsAuthClient;
import com.ecwid.consul.v1.ConsulClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author luoguangming
 */
@Configuration
@ConditionalOnConsulEnabled
@ConditionalOnProperty(name = "spring.cloud.consul.config.enabled", matchIfMissing = true)
public class ConsulConfigAutoConfiguration {

    @Configuration
    @ConditionalOnClass(RefreshEndpoint.class)
    protected static class ConsulRefreshConfiguration {

        @Bean
        @ConditionalOnProperty(name = "spring.cloud.consul.config.watch.enabled",
                matchIfMissing = true)
        public ConfigWatch configWatch(ConsulConfigProperties properties, BmsAuthClient bmsAuthClient,
                                       ConsulPropertySourceLocator locator, ConsulClient consul) {
            return new ConfigWatch(properties, consul, bmsAuthClient, locator.getContextIndexes());
        }

    }

}
