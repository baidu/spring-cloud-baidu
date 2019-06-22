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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure;

import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.exception.BlockException;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.ratelimiter.RateLimiterManager;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * Created by liuruisen on 2019/1/6.
 * springmvc method ratelimiter
 */
@Aspect
public class RateLimiterEffectiveAspect implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterAutoConfiguration.class);

    private RateLimiterManager rateLimiterManager;


    public RateLimiterEffectiveAspect(RateLimiterManager rateLimiterManager) {
        this.rateLimiterManager = rateLimiterManager;
    }


    @Pointcut(value = "@within(org.springframework.web.bind.annotation.RestController) "
            + " || @within(org.springframework.stereotype.Controller)")
    public void controllerPointCut() {

    }

    @Around("controllerPointCut()")
    public Object rateLimiterAroudAdvice(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();

        // method name is the same as rateLimiter name
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        logger.debug("The interception method is {} ", methodName);

        return handleJoinPoint(methodName, proceedingJoinPoint);
    }


    @Override
    public int getOrder() {
        return 2000;
    }


    private Object handleJoinPoint(String methodName, ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        try {
            // refresh rateLimiter for method
            rateLimiterManager.refreshRateLimiter(methodName, 2);

            // waitForPermit
            RateLimiter rateLimiter = rateLimiterManager.getRateLimiterFromRegistry(methodName);
            rateLimiterManager.waitForPermit(rateLimiter);
        } catch (Exception e) {
            if (e instanceof BlockException) {
                throw e;
            } else {
                logger.error("There is some error in RateLimiter");
            }
        }

        // process and return
        try {
            return proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            logger.error(throwable.getMessage());
            throw throwable;
        }
    }

}
