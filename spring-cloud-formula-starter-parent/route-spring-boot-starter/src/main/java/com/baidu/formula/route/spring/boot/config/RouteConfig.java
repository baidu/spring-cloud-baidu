/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
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
package com.baidu.formula.route.spring.boot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.baidu.formula.route.spring.boot.route.RouteMatcher;
import com.baidu.formula.route.spring.boot.route.listener.RouteListener;
import com.baidu.formula.route.spring.boot.route.env.RoutePropertySource;
import com.baidu.formula.route.spring.boot.route.property.RouteProperties;

@Configuration
@RibbonClients(defaultConfiguration = com.baidu.formula.route.spring.boot.route.loadbalancer.CustomIloadBalancer.class)
@EnableConfigurationProperties(RouteProperties.class)
public class RouteConfig {

    @Bean
    public RouteMatcher routeMatcher(RouteProperties routeProperties) {
        return new RouteMatcher(routeProperties);
    }

    @Bean
    public RouteListener routeListener(ApplicationContext applicationContext, SpringClientFactory springClientFactory,
                                       RouteProperties routeProperties, ConfigurableEnvironment environment,
                                       RouteMatcher routeMatcher) {
        return new RouteListener(applicationContext, springClientFactory, routeProperties, environment, routeMatcher);
    }

    //@Bean
    public RoutePropertySource loadBalancePropertySource() {
        return new RoutePropertySource();
    }
}
