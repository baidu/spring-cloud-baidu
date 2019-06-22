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
package com.baidu.formula.route.spring.boot.model.discovery;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Instance {
    public static final int STATUS_UP = 1;
    public static final int STATUS_DOWN = 0;

    private String instanceId;
    private String productName;
    private String environment;
    private String region;
    private String appName;

    private String scheme;
    private String host;
    private Integer port;
    private String path;

    private String type;

    /**
     * 扩展字段
     */
    private Map<String, String> customs = new LinkedHashMap<>();
    private Integer status;

    /**
     * 实例API中的idc
     */
    private String zone;

    private List<String> tags;

    private Date startTime;
}

