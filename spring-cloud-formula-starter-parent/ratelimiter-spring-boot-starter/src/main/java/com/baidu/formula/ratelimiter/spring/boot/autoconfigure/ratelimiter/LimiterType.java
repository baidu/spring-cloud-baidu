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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.ratelimiter;

/**
 * Created by liuruisen on 2019/1/20.
 */
public enum LimiterType {

    TokenBucket(1), Thread(2), Count(3);

    private Integer limiterType;



    LimiterType(Integer limiterType) {
        this.limiterType = limiterType;
    }


    public Integer getLimiterType() {
        return limiterType;
    }

    public void setLimiterType(Integer limiterType) {
        this.limiterType = limiterType;
    }

}
