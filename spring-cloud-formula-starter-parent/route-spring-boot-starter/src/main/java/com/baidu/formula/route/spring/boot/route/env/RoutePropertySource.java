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
package com.baidu.formula.route.spring.boot.route.env;

import static com.baidu.formula.route.spring.boot.config.RouteConstants.ROUTE_PROPERTY_SOURCE;

import java.util.Map;

import org.springframework.core.env.MapPropertySource;

import com.google.common.collect.Maps;

public class RoutePropertySource extends MapPropertySource {

    public RoutePropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    public RoutePropertySource() {
        this(ROUTE_PROPERTY_SOURCE, Maps.newHashMap());
    }

}
