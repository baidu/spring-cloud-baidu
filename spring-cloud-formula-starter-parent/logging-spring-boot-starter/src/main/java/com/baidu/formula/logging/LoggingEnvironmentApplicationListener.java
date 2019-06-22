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
package com.baidu.formula.logging;

import com.baidu.formula.logging.logback.FormulaLogbackSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Bowu Dong (tq02ksu@gmail.com) 2018/3/28.
 */
public class LoggingEnvironmentApplicationListener implements GenericApplicationListener {
    private static final Logger logger = LoggerFactory.getLogger(LoggingEnvironmentApplicationListener.class);

    private int order = Ordered.HIGHEST_PRECEDENCE + 19;

    private static final Class<?>[] EVENT_TYPES = { ApplicationStartingEvent.class,
            ApplicationEnvironmentPreparedEvent.class, ApplicationPreparedEvent.class,
            ContextClosedEvent.class, ApplicationFailedEvent.class };

    private static final Class<?>[] SOURCE_TYPES = { SpringApplication.class,
            ApplicationContext.class };

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationStartingEvent) {
            onApplicationStartingEvent((ApplicationStartingEvent) event);
        }
        else if (event instanceof ApplicationEnvironmentPreparedEvent) {
            onApplicationEnvironmentPreparedEvent(
                    (ApplicationEnvironmentPreparedEvent) event);
        }
    }

    private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
    }


    /**
     * deducing groupId, artifactId
     *
     * @param event
     */
    private void onApplicationStartingEvent(ApplicationStartingEvent event) {
        if (ClassUtils.isPresent("ch.qos.logback.core.Appender",
                event.getSpringApplication().getClassLoader())) {
            // base package
            Class<?> mainClass = event.getSpringApplication().getMainApplicationClass();
            if (mainClass != null) {
                String basePackage = mainClass.getPackage().getName();
                System.setProperty("BASE_PACKAGE", basePackage);
            } else {
                System.setProperty("BASE_PACKAGE", "");
                logger.warn("can not set BASE_PACKAGE correctly");
            }

            // set logging system impl
            System.setProperty(LoggingSystem.SYSTEM_PROPERTY, FormulaLogbackSystem.class.getName());
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
        if (type != null) {
            for (Class<?> supportedType : supportedTypes) {
                if (supportedType.isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean supportsEventType(ResolvableType resolvableType) {
        return isAssignableFrom(resolvableType.getRawClass(), EVENT_TYPES);
    }

    @Override
    public boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return isAssignableFrom(sourceType, SOURCE_TYPES);
    }
}
