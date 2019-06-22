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
package com.baidu.formula.config.client.spring.boot.autoconfigure.refresh;

import com.baidu.formula.config.client.spring.boot.autoconfigure.MultiModuleConfigServicePropertySourceLocator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@Configuration
@ConditionalOnClass(RefreshScope.class)
@ConditionalOnProperty(name = "spring.cloud.refresh.enabled", matchIfMissing = true)
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
@AutoConfigureAfter(RefreshAutoConfiguration.class)
public class PeriodRefreshAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public PeriodCheckRefreshEventEmitter refreshEventEmitter(ConfigServicePropertySourceLocator locator,
                                                              ContextRefresher refresher, Environment environment) {
        return new PeriodCheckRefreshEventEmitter((MultiModuleConfigServicePropertySourceLocator) locator,
                refresher, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationPropertiesDestructionRebindingHelper configurationPropertiesDestructionRebindingHelper() {
        return new ConfigurationPropertiesDestructionRebindingHelper();
    }
}
