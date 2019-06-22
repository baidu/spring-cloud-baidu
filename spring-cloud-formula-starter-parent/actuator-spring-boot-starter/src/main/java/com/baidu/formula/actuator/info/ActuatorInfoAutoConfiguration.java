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
package com.baidu.formula.actuator.info;

import com.baidu.formula.actuator.info.launcher.LauncherInfoContributor;
import com.baidu.formula.actuator.info.launcher.LauncherInfoProperties;
import org.springframework.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor;
import org.springframework.boot.actuate.info.InfoPropertiesInfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Properties;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@Configuration
@EnableConfigurationProperties({ExtraInfoContributorProperties.class})
public class ActuatorInfoAutoConfiguration {
    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    final ExtraInfoContributorProperties properties;

    public ActuatorInfoAutoConfiguration(ExtraInfoContributorProperties properties) {
        this.properties = properties;
    }

    @ConditionalOnProperty(prefix = "", value = "", havingValue = "", matchIfMissing = true)
    @ConditionalOnMissingBean
    @Bean
    public LauncherInfoProperties launcherInfoProperties(Environment environment) {
        Properties map = new Properties();
        Binder.get(environment)
                .bind("formula.launcher", Bindable.mapOf(String.class, String.class))
                .orElseGet(Collections::emptyMap).forEach(map::put);

        if (properties.getLauncher().mode == InfoPropertiesInfoContributor.Mode.SIMPLE) {

        }

        return new LauncherInfoProperties(map);
    }

    @Bean
    @ConditionalOnEnabledInfoContributor("launcher")
    @ConditionalOnSingleCandidate(LauncherInfoProperties.class)
    @ConditionalOnMissingBean
    @Order(DEFAULT_ORDER)
    public LauncherInfoContributor launcherInfoContributor(LauncherInfoProperties properties) {
        return new LauncherInfoContributor(properties, this.properties.getLauncher().getMode());
    }
}
