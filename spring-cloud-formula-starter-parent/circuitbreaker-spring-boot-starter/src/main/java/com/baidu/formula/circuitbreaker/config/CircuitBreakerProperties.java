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
package com.baidu.formula.circuitbreaker.config;

import com.baidu.formula.circuitbreaker.model.CircuitBreakerRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

import static com.baidu.formula.circuitbreaker.config.CircuitBreakerProperties.PREFIX;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@ConfigurationProperties(prefix = PREFIX)
@Data
public class CircuitBreakerProperties {
    public static final String PREFIX = "formula.circuit-breaker";

    private List<CircuitBreakerRule> rules = new ArrayList<>();
}
