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
package com.baidu.formula.circuitbreaker.model;

import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.Data;

/**
 * @author guobolin
 */
@Data
public class TimeLimiterCoalition {

    private TimeLimiter timeLimiter;

    private CircuitBreakerRule rule;

    public TimeLimiterCoalition(TimeLimiter timeLimiter, CircuitBreakerRule rule) {
        this.timeLimiter = timeLimiter;
        this.rule = rule;
    }
}
