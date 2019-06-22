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
package com.baidu.formula.route.spring.boot.route.property;

import java.util.List;

import lombok.Data;

@Data
public class FormulaRouteProperty {
    // db 中id
    private int routingRuleId;

    // 来源服务名
    private String sourceServiceName;

    // 来源工作空间id
    private String sourceWorkspaceId;

    // 流量来源说明
    private Source source;

    // 目标服务名
    private String destServiceName;

    // 目标工作空间id
    private String destWorkspaceId;

    // 目标类型,如weight权重
    private String destType;

    // 流量去向说明
    private List<DestinationWrapper> destinations;

    // 负载均衡策略
    private String loadbalance;

}
