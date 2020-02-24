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
package com.baidubce.formula.consul.config.spring.boot;

import com.baidubce.formula.consul.config.spring.boot.auth.BmsAuthClient;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author luoguangming
 */
@Order(0)
public class ConsulPropertySourceLocator implements PropertySourceLocator {

    private static final Logger logger = LoggerFactory.getLogger(ConsulPropertySourceLocator.class);

    private final ConsulClient consul;

    private final BmsAuthClient bmsAuthClient;

    private final ConsulConfigProperties properties;

    private final List<String> contexts = new ArrayList<>();

    private final LinkedHashMap<String, Long> contextIndex = new LinkedHashMap<>();

    public ConsulPropertySourceLocator(ConsulClient consul,
                                       BmsAuthClient bmsAuthClient,
                                       ConsulConfigProperties properties) {
        this.consul = consul;
        this.bmsAuthClient = bmsAuthClient;
        this.properties = properties;
    }

    @Deprecated
    public List<String> getContexts() {
        return this.contexts;
    }

    public LinkedHashMap<String, Long> getContextIndexes() {
        return this.contextIndex;
    }

    @Override
    @Retryable(interceptor = "consulRetryInterceptor")
    public PropertySource<?> locate(Environment environment) {
        if (environment instanceof ConfigurableEnvironment) {

            List<String> systemLabels = null;
            String rawLabels = properties.getSystemLabels();
            if (!StringUtils.isEmpty(rawLabels)) {
                systemLabels = Arrays.asList(StringUtils.commaDelimitedListToStringArray(
                        StringUtils.trimAllWhitespace(rawLabels)));
            }

            List<String> suffixes = new ArrayList<>();
            if (Format.FILES != this.properties.getFormat()) {
                suffixes.add("/");
            } else {
                suffixes.add(".yml");
                suffixes.add(".yaml");
                suffixes.add(".properties");
            }

            String prefix = this.properties.getPrefix();
            String defaultContext = getContext(prefix,
                    this.properties.getDefaultContext());

            for (String suffix : suffixes) {
                this.contexts.add(defaultContext + suffix);
            }

            if (null != systemLabels && systemLabels.size() != 0) {
                for (String suffix : suffixes) {
                    addLabels(this.contexts, prefix, systemLabels, suffix);
                }
            }

            Collections.reverse(this.contexts);
            logger.debug("Contexts to be pull is: " + contexts.toArray().toString());

            CompositePropertySource composite = new CompositePropertySource("consul");

            for (String propertySourceContext : this.contexts) {
                try {
                    ConsulPropertySource propertySource = null;
                    if (Format.FILES == this.properties.getFormat()) {
                        Response<GetValue> response = this.consul.getKVValue(
                                propertySourceContext, this.bmsAuthClient.getToken());
                        addIndex(propertySourceContext, response.getConsulIndex());
                        if (response.getValue() != null) {
                            ConsulFilesPropertySource filesPropertySource = new ConsulFilesPropertySource(
                                    propertySourceContext, this.consul, this.bmsAuthClient, this.properties);
                            filesPropertySource.init(response.getValue());
                            propertySource = filesPropertySource;
                        }
                    } else {
                        propertySource = create(propertySourceContext, this.contextIndex);
                    }
                    if (propertySource != null) {
                        composite.addPropertySource(propertySource);
                    }
                } catch (Exception e) {
                    if (this.properties.isFailFast()) {
                        logger.error(
                                "Fail fast is set and there was an error reading configuration from consul.");
                        ReflectionUtils.rethrowRuntimeException(e);
                    } else {
                        logger.warn("Unable to load consul config from "
                                + propertySourceContext, e);
                    }
                }
            }

            return composite;
        }
        return null;
    }

    private String getContext(String prefix, String context) {
        if (StringUtils.isEmpty(prefix)) {
            return context;
        } else {
            return prefix + "/" + context;
        }
    }

    private void addIndex(String propertySourceContext, Long consulIndex) {
        this.contextIndex.put(propertySourceContext, consulIndex);
    }

    private ConsulPropertySource create(String context, Map<String, Long> contextIndex) {
        ConsulPropertySource propertySource = new ConsulPropertySource(context,
                this.consul, this.bmsAuthClient, this.properties);
        propertySource.init();
        addIndex(context, propertySource.getInitialIndex());
        return propertySource;
    }

    private void addLabels(List<String> contexts, String prefix,
                           List<String> labels, String suffix) {
        for (String label : labels) {
            contexts.add(prefix + "/" + label + suffix);
        }
    }

}
