/*
 * Copyright 2013-2014 Spring Cloud Config Authors. All rights reserved.
 *
 * Modifications Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.formula.config.client.spring.boot.autoconfigure;

import com.baidu.formula.config.client.spring.boot.autoconfigure.expression.ConfigEnvironmentPreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigClientStateHolder;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.cloud.config.client.ConfigClientProperties.*;

/**
 * PropertySourceLocator implementation support multi module config inheritance.
 * Making this by Extending ConfigServicePropertySourceLocator and override locate method,
 *
 *
 * @author Bowu Dong (tq02ksu@gmail.com) 11/01/2018
 */
public class MultiModuleConfigServicePropertySourceLocator extends ConfigServicePropertySourceLocator {
    private static final Logger logger = LoggerFactory.getLogger(MultiModuleConfigServicePropertySourceLocator.class);

    private static final String APPLICATION_NAME_KEY = "com.baidu.formula.config.application.name";

    private static final String APPLICATION_NAME_ORDER = "com.baidu.formula.config.application.order";

    private List<ModuleConfiguration> depApplications;

    private ConfigClientProperties defaultProperties;

    private RestTemplate restTemplate;

    private ConfigEnvironmentPreprocessor preprocessor;

    private volatile Map<String, String> versions = new ConcurrentHashMap<>();

    public MultiModuleConfigServicePropertySourceLocator(ConfigClientProperties defaultProperties) {
        super(defaultProperties);
        this.defaultProperties = defaultProperties;

        // init depNames:
        this.depApplications = loadApplicationNames(Thread.currentThread().getContextClassLoader());
        depApplications.sort(Comparator.comparingInt(ModuleConfiguration::getOrder));
    }

    public static List<ModuleConfiguration> loadApplicationNames(ClassLoader classLoader) {
        try {
            Enumeration<URL> urls = (classLoader != null ?
                    classLoader.getResources(SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION) :
                    ClassLoader.getSystemResources(SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION));
            return Collections.list(urls).stream()
                    .map(MultiModuleConfigServicePropertySourceLocator::loadProperties)
                    .filter(properties ->  properties.getProperty(APPLICATION_NAME_KEY) != null)
                    .flatMap(props -> {
                        String applicationName = props.getProperty(APPLICATION_NAME_KEY);
                        String order = props.getProperty(APPLICATION_NAME_ORDER);
                        return Stream.of(StringUtils.commaDelimitedListToStringArray(applicationName))
                                .map(name ->
                                        new ModuleConfiguration(name, order == null ? 0 : Integer.parseInt(order)));
                    })
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load multi module configuration " +
                    "from location [" + SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION + "]", ex);
        }
    }

    private static Properties loadProperties(URL url) {
        try {
            return PropertiesLoaderUtils.loadProperties(new UrlResource(url));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load properties " +
                    "from location [" + SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION + "]", ex);
        }
    }

    @Override
    @Retryable(interceptor = "configServerRetryInterceptor")
    public org.springframework.core.env.PropertySource<?> locate(
            org.springframework.core.env.Environment environment) {
        ConfigClientProperties properties = this.defaultProperties.override(environment);
        CompositePropertySource composite = new CompositePropertySource("configService");
        RestTemplate restTemplate = this.restTemplate == null ? getSecureRestTemplate(properties) : this.restTemplate;

        addConfigLocationFiles((ConfigurableEnvironment) environment, composite);

        Exception error = null;
        String errorBody = null;
        logger.info("Fetching config from servers at: " + Arrays.asList(properties.getUri()));
        try {
            String[] labels = new String[] { "" };
            if (StringUtils.hasText(properties.getLabel())) {
                labels = StringUtils.commaDelimitedListToStringArray(properties.getLabel());
            }

            String state = ConfigClientStateHolder.getState();

            // Try all the labels until one works
            Optional<Environment> optionalResult = Arrays.stream(labels).map(label ->
                    getRemoteEnvironment(restTemplate, properties, label.trim(), state)
            ).filter(Objects::nonNull).findFirst();

            if (optionalResult.isPresent()) {
                Environment result = optionalResult.get();
                logger.info(String.format("Located environment: name=%s, profiles=%s, label=%s, version=%s, state=%s",
                        result.getName(),
                        result.getProfiles() == null ? "" : Arrays.asList(result.getProfiles()),
                        result.getLabel(), result.getVersion(), result.getState()));
                recordVersion(result);

                // process application result
                processApplicationResult(composite, result);

                // process other applications
                loadDepEnvironments(environment, composite, state);

                if (StringUtils.hasText(result.getState()) || StringUtils.hasText(result.getVersion())) {
                    HashMap<String, Object> map = new HashMap<>();
                    putValue(map, "config.client.state", result.getState());
                    putValue(map, "config.client.version", result.getVersion());
                    composite.addFirstPropertySource(new MapPropertySource("configClient", map));
                }

                // fix a bug for spring.cloud.config.allow-override property item not work
                // https://github.com/spring-cloud/spring-cloud-config/issues/991
                if (environment instanceof ConfigurableEnvironment) {
                    org.springframework.core.env.PropertySource<?> bootstrapPropertySource =
                            ((ConfigurableEnvironment) environment).getPropertySources()
                                    .get("applicationConfig: [classpath:/bootstrap.properties]");
                    if (bootstrapPropertySource != null) {
                        composite.addPropertySource(bootstrapPropertySource);
                    }
                }
                return composite;
            }
        } catch (HttpServerErrorException e) {
            error = e;
            if (MediaType.APPLICATION_JSON.includes(Objects.requireNonNull(e.getResponseHeaders())
                    .getContentType())) {
                errorBody = e.getResponseBodyAsString();
            }
        } catch (Exception e) {
            error = e;
        }
        if (properties.isFailFast()) {
            throw new IllegalStateException(
                    "Could not locate PropertySource and the fail fast property is set, failing",
                    error);
        }
        logger.warn("Could not locate PropertySource: "
                + (errorBody == null ? error==null ? "label not found" : error.getMessage() : errorBody));
        return null;
    }

    private void processApplicationResult(CompositePropertySource composite, Environment result) {
        if (result.getPropertySources() != null) { // result.getPropertySources() can be null if using xml
            for (PropertySource source : result.getPropertySources()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) source
                        .getSource();
                composite.addPropertySource(new MapPropertySource(source
                        .getName(), map));
            }
        }
    }

    private void recordVersion(Environment result) {
        versions.put(result.getName(), result.getVersion());
    }

    private void addConfigLocationFiles(ConfigurableEnvironment environment, CompositePropertySource composite) {
        MutablePropertySources ps = environment.getPropertySources();
        for (org.springframework.core.env.PropertySource<?> propertySource : ps) {
            if (propertySource.getName().startsWith("applicationConfig: [file:")) {
                logger.info("Adding {} to Cloud Config Client PropertySource", propertySource.getName());
                composite.addPropertySource(propertySource);
            }
        }
    }

    private void loadDepEnvironments(org.springframework.core.env.Environment environment, CompositePropertySource composite, String state) {
        depApplications.forEach(dep -> {
            ConfigClientProperties properties = this.defaultProperties.override(environment);
            properties.setName(dep.getApplicationName());
            RestTemplate restTemplate = this.restTemplate == null ? getSecureRestTemplate(properties) : this.restTemplate;
            Environment result = getRemoteEnvironment(restTemplate, properties, "master", state);
            if (result != null) {
                logger.info(String.format("Located environment: name=%s, profiles=%s, label=%s, version=%s, state=%s",
                        result.getName(),
                        result.getProfiles() == null ? "" : Arrays.asList(result.getProfiles()),
                        result.getLabel(), result.getVersion(), result.getState()));

                processApplicationResult(composite, result);
            }
        });
    }

    private RestTemplate getSecureRestTemplate(ConfigClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (properties.getRequestReadTimeout() < 0) {
            throw new IllegalStateException("Invalid Value for Real Timeout set.");
        }

        requestFactory.setReadTimeout(properties.getRequestReadTimeout());
        RestTemplate template = new RestTemplate(requestFactory);
        Map<String, String> headers = new HashMap<>(properties.getHeaders());
        headers.remove(AUTHORIZATION); // To avoid redundant addition of header

        if (!headers.isEmpty()) {
            template.setInterceptors(Collections.singletonList(new GenericRequestHeaderInterceptor(headers)));
        }

        return template;
    }

    private Environment getRemoteEnvironment(RestTemplate restTemplate, ConfigClientProperties properties,
                                             String label, String state) {
        String path = "/{name}/{profile}";
        String name = properties.getName();
        String profile = properties.getProfile();
        String token = properties.getToken();
        int noOfUrls = properties.getUri().length;

        Object[] args = new String[] { name, profile };
        if (StringUtils.hasText(label)) {
            if (label.contains("/")) {
                label = label.replace("/", "(_)");
            }

            args = new String[] { name, profile, label };
            path = path + "/{label}";
        }
        ResponseEntity<Environment> response = null;

        for (int i = 0; i < noOfUrls; i++) {
            ConfigClientProperties.Credentials credentials = properties.getCredentials(i);
            String uri = credentials.getUri();
            String username = credentials.getUsername();
            String password = credentials.getPassword();

            logger.info("Fetching config from server at : " + uri);

            try {
                HttpHeaders headers = new HttpHeaders();
                addAuthorizationToken(properties, headers, username, password);

                if (StringUtils.hasText(token)) {
                    headers.add(TOKEN_HEADER, token);
                }
                if (StringUtils.hasText(state) && properties.isSendState()) {
                    headers.add(STATE_HEADER, state);
                }

                final HttpEntity<Void> entity = new HttpEntity<>((Void) null, headers);
                response = restTemplate.exchange(uri + path, HttpMethod.GET,
                        entity, Environment.class, args);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                    throw e;
                }
            } catch (ResourceAccessException e) {
                logger.info("Connect Timeout Exception on Url - " + uri
                        + ". Will be trying the next url if available");
                if (i == noOfUrls - 1) {
                    throw e;
                } else {
                    continue;
                }
            }

            if (response == null || response.getStatusCode() != HttpStatus.OK) {
                return null;
            }

            return postProcessResult(response.getBody());
        }

        return null;
    }

    private Environment postProcessResult(Environment result) {
        if (preprocessor == null) {
            return result;
        }

        result.getPropertySources().forEach(ps -> {
            Map<?, ?> source = ps.getSource();

            new HashSet<>(source.keySet()).forEach(key -> {
                Object value = ps.getSource().get(key);
                if (key instanceof String && value instanceof String) {
                    String processedKey = preprocessor.process((String) key);
                    String processedValue = preprocessor.process((String) value);

                    if (!key.equals(processedKey) || !value.equals(processedValue)) {
                        logger.info("preprocessor processed config: key={}, value={}, " +
                                "REPLACED KEY={}, VALUE={}", key, value, processedKey, processedValue);
                        ps.getSource().remove(key);
                        ((Map<Object, Object>) source).put(processedKey, processedValue);
                    }
                }
            });
        });
        return result;
    }

    private void addAuthorizationToken(ConfigClientProperties configClientProperties,
                                       HttpHeaders httpHeaders, String username, String password) {
        String authorization = configClientProperties.getHeaders().get(AUTHORIZATION);

        if (password != null && authorization != null) {
            throw new IllegalStateException("You must set either 'password' or 'authorization'");
        }

        if (password != null) {
            byte[] token = Base64Utils.encode((username + ":" + password).getBytes());
            httpHeaders.add("Authorization", "Basic " + new String(token));
        } else if (authorization != null) {
            httpHeaders.add("Authorization", authorization);
        }
    }

    private void putValue(HashMap<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }

    public void setPreprocessor(ConfigEnvironmentPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    @Override
    public void setRestTemplate(RestTemplate restTemplate) {
        super.setRestTemplate(restTemplate);
        this.restTemplate = restTemplate;
    }

    public Map<String, String> getVersions() {
        return new HashMap<>(versions);
    }
}
