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
package com.baidu.formula.config.client.spring.boot.autoconfigure;

import com.baidu.formula.config.client.spring.boot.autoconfigure.expression.ConfigEnvironmentPreprocessor;
import com.baidu.formula.config.client.spring.boot.autoconfigure.expression.Jexl3ConfigEnvironmentPreprocessor;
import org.apache.commons.jexl3.JexlEngine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Bowu Dong (tq02ksu@gmail.com) 11/01/2018
 */
@Configuration
@EnableConfigurationProperties
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class MultiModuleConfigServiceBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConfigServicePropertySourceLocator.class)
    @ConditionalOnProperty(value = "spring.cloud.config.enabled", matchIfMissing = true)
    public ConfigServicePropertySourceLocator configServicePropertySource(
            ConfigClientProperties client, ObjectProvider<ConfigEnvironmentPreprocessor> preprocessorObjectProvider) {
        MultiModuleConfigServicePropertySourceLocator l = new MultiModuleConfigServicePropertySourceLocator(client);
        preprocessorObjectProvider.ifAvailable(l::setPreprocessor);
        return l;
    }

    @Configuration
    @ConditionalOnClass(JexlEngine.class)
    static class Jexl3 {
        @Bean
        @ConditionalOnMissingBean
        public ConfigEnvironmentPreprocessor configEnvironmentPreprocessor() {
            return new Jexl3ConfigEnvironmentPreprocessor();
        }
    }
}
