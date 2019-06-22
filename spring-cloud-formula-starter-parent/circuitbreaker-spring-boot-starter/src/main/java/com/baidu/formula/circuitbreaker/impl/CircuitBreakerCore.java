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

import com.baidu.formula.circuitbreaker.enumeration.FallbackTypeEnum;
import com.baidu.formula.circuitbreaker.exception.CircuitBreakerOpenException;
import com.baidu.formula.circuitbreaker.exception.FallBackNotFoundException;
import com.baidu.formula.circuitbreaker.fallback.ObjectMapperCallable;
import com.baidu.formula.circuitbreaker.model.CircuitBreakerCoalition;
import com.baidu.formula.circuitbreaker.model.CircuitBreakerRule;
import com.baidu.formula.circuitbreaker.model.TimeLimiterCoalition;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class CircuitBreakerCore {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerCore.class);

    private static final ConcurrentHashMap<MethodKey, Optional<Method>> fallbackMethodCache = new ConcurrentHashMap<>();

    private final ExecutorService executorService;

    private final CircuitBreakerManager manager;

    public CircuitBreakerCore(CircuitBreakerManager manager) {
        this.manager = manager;
        CustomizableThreadFactory factory = new CustomizableThreadFactory();
        factory.setDaemon(true);
        factory.setThreadNamePrefix("circuit-breaker-");
        executorService = Executors.newCachedThreadPool(factory);
    }

    public Object process(Method method, Object target, Object[] args) throws Exception {
        String name = getName(method);
        return process("", name, "", method, target, args);
    }

    public Object process(String httpMethod, String serviceName, String url,
                          Method method, Object target, Object[] args) throws Exception {
        // 先获取最符合要求的熔断规则
        CircuitBreakerCoalition circuitBreakerCoalition =
                manager.getCircuitBreakerCoalition(httpMethod, serviceName, url);
        CircuitBreaker circuitBreaker = null;
        CircuitBreakerRule rule = null;
        String name = null;
        if (circuitBreakerCoalition != null) {
            circuitBreaker = circuitBreakerCoalition.getCircuitBreaker();
            rule = circuitBreakerCoalition.getRule();
            name = rule.getRuleName();
        }

        // 超时后续可能支持
        TimeLimiterCoalition timeLimiterCoalition =
                manager.getTimeLimiterCoalition(httpMethod, serviceName, url);
        TimeLimiter timeLimiter = null;
        Callable<Object> callable =
                () -> {
                    try {
                        method.setAccessible(true);
                        return method.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw deduceCauseException(e);
                    }
                };

        if (timeLimiter != null) {
            Callable<Object> finalCallable = callable;
            callable = () -> {
                Supplier<Future<Object>> futureSupplier = () -> executorService.submit(finalCallable);
                // Wrap your call to BackendService.doSomething() in a future provided by your executor
                Callable<Object> result = TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier);
                try {
                    return result.call();
                } catch (ExecutionException e) {
                    throw deduceCauseException(e);
                }
            };
        }

        if (circuitBreaker == null) {
            return callable.call();
        } else if (!circuitBreaker.isCallPermitted()) {
            logger.info("CircuitBreaker[{}] is open, go to fallback invocation", name);
            Callable<Object> fallbackCall = null;
            if (rule != null) {
                fallbackCall = getFallback(rule, method, rule.getFallbackResult(), target, args, null);
            }
            if (fallbackCall != null) {
                return fallbackCall.call();
            } else {
                throw new CircuitBreakerOpenException("CircuitBreaker is open");
            }
        }

        // 命中熔断规则，但熔断器不处于open状态
        try {
            logger.info("CircuitBreaker[{}] is match", name);
            return circuitBreaker.executeCallable(callable);
        } catch (Throwable e) {
            logger.warn("Method[{}#{}] invocation failed due to [{}:{}], executing fallback...",
                    method.getDeclaringClass().getSimpleName(), method.getName(),
                    e.getClass(), e.getMessage(), e);
            Callable<Object> fallbackCall = null;
            if (rule != null) {
                fallbackCall = getFallback(rule, method, rule.getFallbackResult(), target, args, e);
            }
            if (fallbackCall != null) {
                return fallbackCall.call();
            } else {
                throw getException(e);
            }
        }

    }

    private Exception getException(Throwable e) {
        if (e instanceof Exception) {
            return (Exception) e;
        } else {
            return new Exception(e.getMessage(), e);
        }
    }

    private Exception deduceCauseException(Exception e) {
        if (e.getCause() != null && e.getCause() instanceof Exception) {
            return (Exception) e.getCause();
        }

        return e;
    }

    /**
     * 当前只支持返回熔断异常
     * 后续预计支持null和url级别
     *
     * @param rule
     * @param method
     * @param fallbackResult
     * @param target
     * @param args
     * @param t
     * @return
     */
    private Callable<Object> getFallback(CircuitBreakerRule rule, Method method, String fallbackResult,
                                         Object target, Object[] args, Throwable t) {
        if (rule == null) {
            return null;
        }

        Integer fallbackType = rule.getFallbackType();
        if (fallbackType != null) {
            if (FallbackTypeEnum.EXCEPTION.equals(FallbackTypeEnum.getById(fallbackType))) {
                return () -> {
                    if (t == null) {
                        logger.info("fallback value is CircuitBreakerOpenException.");
                        throw new CircuitBreakerOpenException("CircuitBreaker is open");
                    } else {
                        throw getException(t);
                    }
                };
            } else if (FallbackTypeEnum.NULL.equals(FallbackTypeEnum.getById(fallbackType))) {
                return () -> {
                    logger.info("fallback value is null.");
                    return null;
                };
            }
        }

        if (rule.getFallbackResult() != null) {
            return new ObjectMapperCallable(rule.getFallbackResult(), method);
        }
        com.baidu.formula.circuitbreaker.annotation.CircuitBreaker annotation =
                AnnotationUtils.findAnnotation(method,
                        com.baidu.formula.circuitbreaker.annotation.CircuitBreaker.class);
        if (annotation == null || annotation.fallback().isEmpty()) {
            return () -> {
                throw new FallBackNotFoundException(
                        String.format("CircuitBreaker[%s]'s fallback method not configured.", rule));
            };
        }

        Method fallbackMethod = findFallbackMethod(method, fallbackResult, target.getClass(),
                t == null ? null : t.getClass(), annotation);

        if (fallbackMethod == null) {
            return () -> new FallBackNotFoundException(
                    String.format("CircuitBreaker[%s](%s)'s fallback method not found.",
                            rule, annotation));
        }

        if (fallbackMethod.getParameterCount() - method.getParameterCount() == 1) {
            List<Object> argList = new ArrayList<>(Arrays.asList(args));
            argList.add(t);
            args = argList.toArray();
        }

        Object[] finalArgs = args;
        return () -> process(fallbackMethod, target, finalArgs);
    }

    public Method findFallbackMethod(Method method, String fallbackResult,
                                     Class<?> targetClass, Class<? extends Throwable> throwableClass,
                                     com.baidu.formula.circuitbreaker.annotation.CircuitBreaker annotation) {
        MethodKey key = MethodKey.builder()
                .method(method)
                .fallBackResult(fallbackResult)
                .targetClass(targetClass)
                .throwableClass(throwableClass)
                .fallbackMethodName(annotation.fallback()).build();
        if (!fallbackMethodCache.containsKey(key)) {
            Method m = findFallbackMethodInternal(method, targetClass, throwableClass, annotation);

            if (m == null) {
                // find a recent fallback method
                TreeMap<MethodKey, Optional<Method>> treeMap = new TreeMap<>(
                        Comparator.comparing(MethodKey::getTimestamp).reversed());
                fallbackMethodCache.forEach((k, v) -> v.ifPresent(val -> treeMap.put(k, v)));

                m = treeMap.entrySet().stream()
                        .filter(entry -> entry.getKey().getMethod().equals(method))
                        .filter(entry -> entry.getKey().getTargetClass().equals(targetClass))
                        .map(entry -> entry.getValue().orElse(null))
                        .findFirst().orElse(null);
            }

            fallbackMethodCache.putIfAbsent(key, Optional.ofNullable(m));
        }
        return fallbackMethodCache.get(key).orElse(null);
    }

    public Method findFallbackMethodInternal(Method method, Class<?> targetClass,
                                             Class<? extends Throwable> throwableClass,
                                             com.baidu.formula.circuitbreaker.annotation.CircuitBreaker annotation) {
        for (; throwableClass != null && Throwable.class.isAssignableFrom(throwableClass);
             throwableClass = (Class<? extends Throwable>) throwableClass.getSuperclass()) {
            Class<?>[] parameterTypes = Stream.concat(
                    Arrays.stream(method.getParameterTypes()),
                    Stream.of(throwableClass)).toArray(Class<?>[]::new);
            Method fallbackMethod = ReflectionUtils.findMethod(targetClass, annotation.fallback(), parameterTypes);

            if (fallbackMethod != null) {
                return fallbackMethod;
            }

            Class<?>[] interfaces = Arrays.stream(throwableClass.getInterfaces())
                    .filter(Throwable.class::isAssignableFrom)
                    .toArray(Class[]::new);
            for (Class<?> i : interfaces) {
                parameterTypes = Stream.concat(
                        Arrays.stream(method.getParameterTypes()),
                        Stream.of(i)).toArray(Class<?>[]::new);
                fallbackMethod = ReflectionUtils.findMethod(targetClass, annotation.fallback(), parameterTypes);
                if (fallbackMethod != null) {
                    return fallbackMethod;
                }
            }
        }

        return ReflectionUtils.findMethod(targetClass, annotation.fallback(), method.getParameterTypes());
    }

    public String getName(Method m) {
        return m.getDeclaringClass().getName() + "#" + m.getName();
    }

    /**
     * 判断有没有命中的规则
     *
     * @param httpMethod
     * @param serviceName
     * @param url
     * @return
     */
    public Boolean checkRulesExist(String httpMethod, String serviceName, String url) {
        // 获取最符合要求的熔断规则
        CircuitBreakerCoalition circuitBreakerCoalition =
                manager.getCircuitBreakerCoalition(httpMethod, serviceName, url);
        if (circuitBreakerCoalition == null || circuitBreakerCoalition.getRule() == null) {
            return false;
        } else {
            return true;
        }
    }

    @Builder
    @ToString
    @Getter
    @Setter
    @RequiredArgsConstructor
    @EqualsAndHashCode(exclude = "timestamp")
    @AllArgsConstructor
    private static class MethodKey {
        private String fallBackResult;
        private Method method;
        private Class<?> targetClass;
        private Class<? extends Throwable> throwableClass;
        private String fallbackMethodName;

        @Builder.Default
        private long timestamp = System.currentTimeMillis();
    }
}
