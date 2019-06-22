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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config;


import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.entity.FormulaRateLimiterConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Created by liuruisen on 2018/12/27.
 */
// @RefreshScope
@Data
@ConfigurationProperties(prefix = RateLimiterProperties.PREFIX)
public class RateLimiterProperties {

    static final String PREFIX = "formula.ratelimiter";

    private boolean enabled;

    //private Map<String, FormulaRateLimiterConfig> ratelimiters;

    private List<FormulaRateLimiterConfig> ratelimiters;


}
