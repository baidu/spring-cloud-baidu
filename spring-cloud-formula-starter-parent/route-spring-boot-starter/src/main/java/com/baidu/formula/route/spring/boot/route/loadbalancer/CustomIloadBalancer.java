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
package com.baidu.formula.route.spring.boot.route.loadbalancer;

import static com.baidu.formula.route.spring.boot.config.RouteConstants.FORMULA_DISCOVERY_CUSTOM_PLATFORM;
import static com.baidu.formula.route.spring.boot.config.RouteConstants.TAG_PLATFORM;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baidu.formula.route.spring.boot.model.discovery.FormulaDiscoveryServer;
import com.baidu.formula.route.spring.boot.route.RouteMatcher;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ServerListUpdater;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

public class CustomIloadBalancer<T extends Server> extends ZoneAwareLoadBalancer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomIloadBalancer.class);

    @Autowired
    private RouteMatcher routeMatcher;

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public CustomIloadBalancer(IClientConfig config, IRule rule, IPing ping, ServerList serverList,
                               ServerListFilter serverListFilter, ServerListUpdater serverListUpdater) {
        super(config, rule, ping, serverList,
                serverListFilter, serverListUpdater);
    }

    @Override
    public List<Server> getAllServers() {
        List<Server> allServerList =  super.getAllServers();
        return route(allServerList);
    }

    @Override
    public List<Server> getReachableServers() {
        List<Server> upServerList = super.getReachableServers();
        return route(upServerList);
    }

    /**
     * 路由
     * @param list
     * @return
     */
    public List<Server> route(List<Server> list) {
        try {
            if (CollectionUtils.isEmpty(list)) {
                LOGGER.info("instance list is empty.");
                return list;
            }
            LOGGER.info("route info : {}", routeMatcher.getRouteProperties());
            if (!routeMatcher.match()) {
                LOGGER.info("route does not match this instance.");
                return list;
            }
            // 路由对本实例生效时则检查路由是否合法
            routeMatcher.checkRoute();
            // 基于权重选出一个标签对应值
            String tagValue = routeMatcher.getDestinationTagValue();
            // 标签对应的key
            String tagKey = routeMatcher.getRouteProperties().getDestinationTagKey();
            // 实现路由功能
            List<Server> routedList = getRoutedList(list, tagKey, tagValue);
            return routedList;
        } catch (Exception e) {
            LOGGER.error("route exception: {}", e);
            return list;
        }
    }


    public List<Server> getRoutedList(List<Server> list, String tagKey, String expectedTagValue)
            throws Exception {
        if (TAG_PLATFORM.equalsIgnoreCase(tagKey)) {
            List<Server> resultServerList = Lists.newArrayList(list);

            // 对接天路注册中心
            JavaType javaType = getCollectionType(List.class, FormulaDiscoveryServer.class);
            String value = objectMapper.writeValueAsString(resultServerList);
            List<FormulaDiscoveryServer> serverList = objectMapper.readValue(value, javaType);

            Iterator<Server> iterator1 = resultServerList.iterator();
            Iterator<FormulaDiscoveryServer> iterator2 = serverList.iterator();
            while(iterator2.hasNext()) {
                FormulaDiscoveryServer formulaDiscoveryServer = iterator2.next();
                iterator1.next();
                // 平台（部署组信息）
                String platformName = formulaDiscoveryServer.getInstance().getCustoms().get(
                        FORMULA_DISCOVERY_CUSTOM_PLATFORM);
                // 不符合条件的移出掉
                if (StringUtils.isEmpty(platformName) || !platformName.equals(expectedTagValue)) {
                    iterator1.remove();
                }
            }
            // TODO 是否对路由后的实例列表为0时做特殊处理
            return resultServerList;
        }
        return list;
    }


    public JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
}

