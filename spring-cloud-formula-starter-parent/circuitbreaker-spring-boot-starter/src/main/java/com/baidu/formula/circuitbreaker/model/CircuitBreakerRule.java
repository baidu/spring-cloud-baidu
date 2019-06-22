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

import com.baidu.formula.engine.tag.FormulaSource;
import lombok.Data;

import java.time.Duration;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@Data
public class CircuitBreakerRule {
    public static final int DEFAULT_FAILURE_RATE_THRESHOLD = 50;
    public static final int DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE = 10;
    public static final int DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE = 20;
    public static final Duration DEFAULT_WAIT_DURATION_IN_OPEN_STATE = Duration.ofSeconds(60);
    public static final boolean DEFAULT_CANCEL_RUNNING_FUTURE = true;
    public static final int MENTHOD_CIRCUITBREAKER = 3;

    private Long ruleId;

    private Boolean enabled;

    // 请求端的限制规则
    private FormulaSource source;

    private String method;

    private String serviceName;

    // 1:http 2:rpc 3：menthd
    private Integer effectiveType;

    private String effectivePattren;

    private String effectiveLocation;

    private Boolean forceOpen;

    private Integer ringBufferSizeInHalfOpenState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;

    private Integer ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;

    private Integer failureRateThreshold;

    private Duration waitDurationInOpenState = DEFAULT_WAIT_DURATION_IN_OPEN_STATE;

    private Integer fallbackType;

    private String fallbackResult;

    private Duration timeoutDuration;

    private Boolean cancelRunningFuture = DEFAULT_CANCEL_RUNNING_FUTURE;

    public String getRuleName() {
        if (this.effectiveType == MENTHOD_CIRCUITBREAKER) {
            return this.method;
        }else {
            return this.serviceName + this.effectivePattren + this.effectiveLocation;
        }
    }
}
