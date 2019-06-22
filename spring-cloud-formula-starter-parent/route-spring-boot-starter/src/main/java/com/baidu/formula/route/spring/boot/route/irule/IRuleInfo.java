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
package com.baidu.formula.route.spring.boot.route.irule;

public enum  IRuleInfo {

    RANDOM("RANDOM", "com.netflix.loadbalancer.RandomRule", "随机"),
    WEIGHT("WEIGHT", "com.netflix.loadbalancer.WeightedResponseTimeRule", "响应时间权重"),
    ROUDROBIN("ROUDROBIN", "com.netflix.loadbalancer.RoundRobinRule", "简单轮训"),
    ZONEAVOIDANCERULE("ZONEAVOIDANCERULE", "com.netflix.loadbalancer.ZoneAvoidanceRule", "加权轮训");

    private String ruleName;
    private String rulePath;
    private String des;

    IRuleInfo(String ruleName, String rulePath, String des) {
        this.ruleName = ruleName;
        this.rulePath = rulePath;
        this.des = des;
    }

    public static String getRulePath (String ruleName) {
        for (IRuleInfo iRuleInfo : IRuleInfo.values()) {
            if (iRuleInfo.ruleName.equalsIgnoreCase(ruleName)) {
                return iRuleInfo.rulePath;
            }
        }
        throw new IllegalArgumentException("illegal argument rule name: " + ruleName);
    }
}
