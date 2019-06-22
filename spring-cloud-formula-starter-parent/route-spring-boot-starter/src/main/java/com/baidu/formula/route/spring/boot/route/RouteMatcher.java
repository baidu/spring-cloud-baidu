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
package com.baidu.formula.route.spring.boot.route;

import static com.baidu.formula.route.spring.boot.config.RouteConstants.DESTINATION_TYPE_WEIGHT;
import static com.baidu.formula.route.spring.boot.config.RouteConstants.TAG_EQUAL_OPERATOR;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.baidu.formula.route.spring.boot.route.property.DestinationWrapper;
import com.baidu.formula.route.spring.boot.route.property.FormulaRouteProperty;
import com.baidu.formula.route.spring.boot.route.property.RouteProperties;
import com.baidu.formula.route.spring.boot.route.property.Tag;

import lombok.Getter;

/**
 * 路由规则匹配器
 */
@Getter
public class RouteMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteMatcher.class);

    private RouteProperties routeProperties;

    FormulaRouteProperty matchedFormulaRouteProperty;

    public RouteMatcher(RouteProperties routeProperties) {
        this.routeProperties = routeProperties;
    }

    /**
     * 路由规则是否对本实例生效
     * @return true 路由规则匹配该实例，否则返回false.
     */
    public boolean match() {
        if (CollectionUtils.isEmpty(routeProperties.getRules())) {
            LOGGER.info("route rules is empty!");
            return false;
        }
        // 开始匹配是否生效在本实例上，按照先后顺序
        for (FormulaRouteProperty formulaRouteProperty : routeProperties.getRules()) {
            boolean matched = true;
            List<Tag> tags = formulaRouteProperty.getSource().getTags();
            for (Tag tag : tags) {
                // 获取tag在环境变量中的值
                String envValue = System.getenv(tag.getKey());
                if (!isMatch(tag.getOp(), tag.getValue(), envValue)) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                // 找到匹配的路由规则
                this.matchedFormulaRouteProperty = formulaRouteProperty;
                LOGGER.info("find matched route: {}" + matchedFormulaRouteProperty);
                return true;
            }
        }
        return false;

    }

    /**
     * 匹配是否满足逻辑预算
     * @param operator        逻辑运算符
     * @param value           值
     * @param expectedValue   期望的值
     * @return
     */
    public boolean isMatch(String operator, List<String> value, String expectedValue) {
        if (TAG_EQUAL_OPERATOR.equalsIgnoreCase(operator)) {
            return value.contains(expectedValue);
        }
        return false;
    }

    /**
     *  检查路由的规则是否满足合法
     */
    public void checkRoute() {
        int weight = 0;
        // 权重路由时，需保证权重之和为100
        if (DESTINATION_TYPE_WEIGHT.equalsIgnoreCase(this.matchedFormulaRouteProperty.getDestType())) {
            for (DestinationWrapper destinationWrapper : this.matchedFormulaRouteProperty.getDestinations()) {
                weight += destinationWrapper.getDestination().getWeight();
            }
        }
        if (weight != 100) {
            throw new IllegalArgumentException("weight is illegal.");
        }
    }

    public String getDestinationTagValue() {
        List<Pair<String, Integer>> list = new ArrayList<>();
        for (DestinationWrapper destinationWrapper : this.matchedFormulaRouteProperty.getDestinations()) {
            int weight = destinationWrapper.getDestination().getWeight();

            for (Tag tag : destinationWrapper.getDestination().getTags()) {
                list.add(new ImmutablePair(tag.getValue().iterator().next(), weight));
            }
        }
        return new WeightRandom(list).random();
    }



}
