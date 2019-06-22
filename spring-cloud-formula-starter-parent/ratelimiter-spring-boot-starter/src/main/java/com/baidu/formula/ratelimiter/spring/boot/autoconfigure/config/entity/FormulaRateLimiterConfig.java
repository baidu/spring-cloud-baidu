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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.entity;

import com.baidu.formula.engine.tag.FormulaDestination;
import com.baidu.formula.engine.tag.FormulaSource;
import lombok.Data;

/**
 * Created by liuruisen on 2018/12/27.
 */

@Data
public class FormulaRateLimiterConfig implements Cloneable{

    private Long ruleId;


    private FormulaSource source;


    private FormulaDestination destination;


    private Integer effectiveType; // 生效位置的类型 1:uri 2:method


    private String effectiveLocation;


    private Integer limiterType; // 限流类型 1:QPS 2:Thread


    private Boolean enabled; // 该规则是否生效


    private Integer timeoutInMillis = 0;

    // 限流阈值，可以为qps指定, 也可以为thread指定
    private Integer threshold;

    // lowcase
    private String httpMethod;


    public String getLimiterName(){
        String key = null;
        if (effectiveType != null && effectiveType == 1) {
            key = effectiveLocation + "#" + httpMethod.toLowerCase();
        } else if (effectiveType != null && effectiveType == 2) {
            key = effectiveLocation;
        }
        return key;
    }

    @Override
    public Object clone() {
        FormulaRateLimiterConfig configuration = null;
        try {
            configuration = (FormulaRateLimiterConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return configuration;
    }
}
