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
package com.baidu.formula.circuitbreaker.impl;

import com.baidu.formula.circuitbreaker.annotation.CircuitBreaker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@Aspect
public class CircuitBreakerAspect implements Ordered {
    private final CircuitBreakerCore circuitBreakerCore;

    public CircuitBreakerAspect(CircuitBreakerCore circuitBreakerCore) {
        this.circuitBreakerCore = circuitBreakerCore;
    }

    @Pointcut(value = "@within(circuitBreaker) || @annotation(circuitBreaker)", argNames = "circuitBreaker")
    public void pointcut(CircuitBreaker circuitBreaker) {
    }

    @Around(value = "pointcut(backendMonitored)", argNames = "joinPoint, backendMonitored")
    public Object around(ProceedingJoinPoint joinPoint, CircuitBreaker backendMonitored) throws Throwable {
        MethodSignature s = (MethodSignature) joinPoint.getSignature();
        return circuitBreakerCore.process(s.getMethod(), joinPoint.getTarget(), joinPoint.getArgs());
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
