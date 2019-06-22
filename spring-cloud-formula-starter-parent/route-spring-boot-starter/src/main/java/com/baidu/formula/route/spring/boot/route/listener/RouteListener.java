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
package com.baidu.formula.route.spring.boot.route.listener;

import static com.baidu.formula.route.spring.boot.config.RouteConstants.CONFIG_NAMESPACE;
import static com.baidu.formula.route.spring.boot.config.RouteConstants.CONFIG_RULE_CLASS;
import static com.baidu.formula.route.spring.boot.config.RouteConstants.ROUTE_PROPERTY_SOURCE;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;

import com.baidu.formula.route.spring.boot.route.RouteMatcher;
import com.baidu.formula.route.spring.boot.route.env.RoutePropertySource;
import com.baidu.formula.route.spring.boot.route.irule.IRuleInfo;
import com.baidu.formula.route.spring.boot.route.property.FormulaRouteProperty;
import com.baidu.formula.route.spring.boot.route.property.RouteProperties;
import com.google.common.collect.Maps;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.WeightedResponseTimeRule;

/**
 * 路由规则监听器
 */
public class RouteListener implements ApplicationListener<EnvironmentChangeEvent>, ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteListener.class);

    private ApplicationContext applicationContext;

    private SpringClientFactory springClientFactory;

    private RouteProperties routeProperties;

    private ConfigurableEnvironment configurableEnvironment;

    private RouteMatcher routeMatcher;

    public RouteListener(ApplicationContext applicationContext, SpringClientFactory springClientFactory,
                         RouteProperties routeProperties, ConfigurableEnvironment configurableEnvironment,
                         RouteMatcher routeMatcher) {
        this.applicationContext = applicationContext;
        this.springClientFactory = springClientFactory;
        this.routeProperties = routeProperties;
        this.configurableEnvironment = configurableEnvironment;
        this.routeMatcher = routeMatcher;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 监听EnvironmentChangeEvent 事件，更改相关环境变量
     * @param event
     */
    @Override
    @EventListener(EnvironmentChangeEvent.class)
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        try {
            LOGGER.info("environment change.");
            Map<String, Object> propertySource = Maps.newHashMap();
            if (!routeMatcher.match()) {
                LOGGER.info("this route rules does not match this instance.");
                return;
            }
            // 多条路由规则已先后顺序进行匹配
            FormulaRouteProperty formulaRouteProperty = routeMatcher.getMatchedFormulaRouteProperty();
            // 获取新的负载均衡策略
            String iRuleName = formulaRouteProperty.getLoadbalance();
            String destServiceName = formulaRouteProperty.getDestServiceName();
            IRule oldRule = springClientFactory.getInstance(destServiceName, IRule.class);
            if (oldRule instanceof WeightedResponseTimeRule) {
                // 关闭线程池
                ((WeightedResponseTimeRule) oldRule).shutdown();
            }
            // 清理ribbon 中 所有的client的负载均衡器配置，更改环境变量值，等待下次重新加载client的负载均衡配置
            springClientFactory.destroy();

            // 按照ribbon的规范，配置IRule
            String configClientRule = destServiceName + "." + CONFIG_NAMESPACE + "." + CONFIG_RULE_CLASS;

            propertySource.put(configClientRule, IRuleInfo.getRulePath(iRuleName));
            // 加入至环境变量中
            this.configurableEnvironment.getPropertySources().addFirst(new RoutePropertySource(ROUTE_PROPERTY_SOURCE,
                    propertySource));
        } catch (Exception e) {
            LOGGER.error("refresh route rule exception: {}", e);
        }

    }

}

