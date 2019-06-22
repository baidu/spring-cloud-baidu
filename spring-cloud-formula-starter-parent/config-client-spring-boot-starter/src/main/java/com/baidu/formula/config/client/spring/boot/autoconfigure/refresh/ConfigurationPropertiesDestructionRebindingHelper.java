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
package com.baidu.formula.config.client.spring.boot.autoconfigure.refresh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class ConfigurationPropertiesDestructionRebindingHelper implements DestructionAwareBeanPostProcessor {
    private static final Logger logger =
            LoggerFactory.getLogger(ConfigurationPropertiesDestructionRebindingHelper.class);

    @Autowired
    private Environment environment;

    @Autowired
    private ConfigurationPropertiesBeans beans;

    @Override
    public void postProcessBeforeDestruction(Object bean, String name) throws BeansException {
        if (environment == null) {
            return;
        }

        Object target = bean;
        if (AopUtils.isAopProxy(bean)) {
            target = ProxyUtils.getTargetObject(target);
        }

        if (AnnotationUtils.findAnnotation(target.getClass(), ConfigurationProperties.class) == null) {
            return;
        }

        try {
            target.getClass().getConstructor();
        } catch (NoSuchMethodException e) {
            logger.debug("can not found default constructor, skip it");
            return;
        }

        try {
            ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
                    target.getClass(), ConfigurationProperties.class);
            String prefix = annotation.prefix();
            Object result = Binder.get(environment).bind(prefix, (Class) target.getClass()).orElseCreate(target.getClass());
            BeanUtils.copyProperties(result, target);
        } catch (Throwable t) {
            logger.warn("error while process destruction bean with name: {}", name, t);
        }
    }
}
