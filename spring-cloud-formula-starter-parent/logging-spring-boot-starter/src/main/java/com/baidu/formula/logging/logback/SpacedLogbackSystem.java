/*
 * Copyright 2012-2018 Spring Boot Authors. All rights reserved.
 *
 * Modifications copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.formula.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import com.baidu.formula.logging.config.LoggingProperties;
import com.baidu.formula.logging.config.Space;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * Base logback configuration,
 * default log format references to DefaultLogbackConfiguration
 *
 * @see org.springframework.boot.logging.logback.DefaultLogbackConfiguration
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public abstract class SpacedLogbackSystem extends LogbackLoggingSystem {
    private static final Logger logger = LoggerFactory.getLogger(SpacedLogbackSystem.class);

    protected static final String LOGGING_PATTERN_LEVEL =
            "%5p [${spring.cloud.config.name:${spring.application.name:-}},%X{X-B3-TraceId:-},%X{X-B3-SpanId:-},%X{X-Span-Export:-}]";

    protected static final String FILE_LOG_PATTERN =
            "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} "
                    + "${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : "
                    + "%m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}";

    static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    public static final String KEY_ENABLED = "FORMULA_LOGGING_ENABLED";
    protected static final String DEFAULT_PATH = "log";
    protected static final String DEFAULT_FILE_SIZE = "1GB";
    protected static final int DEFAULT_MAX_HISTORY = 24 * 7;
    protected static final String DEFAULT_TOTAL_SIZE_CAP = "30GB";
    protected static final boolean DEFAULT_ADDDTIVITY = false;

    protected LoggerContext context;

    protected LoggingProperties properties;

    protected PropertyResolver patterns;

    protected boolean checkPassed;

    public SpacedLogbackSystem(ClassLoader classLoader) {
        super(classLoader);
    }

    protected PropertyResolver getPatternsResolver(Environment environment) {
        if (environment == null) {
            return new PropertySourcesPropertyResolver(null);
        }
        if (environment instanceof ConfigurableEnvironment) {
            PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
                    ((ConfigurableEnvironment) environment).getPropertySources());
            resolver.setIgnoreUnresolvableNestedPlaceholders(true);
            return resolver;
        }
        return environment;
    }

    protected boolean isSet(ConfigurableEnvironment environment, String property) {
        String value = environment.getProperty(property);
        return (value != null && !value.equals("false"));
    }

    protected LoggingProperties parseProperties(ConfigurableEnvironment environment) {
        LoggingProperties properties = Binder.get(environment)
                .bind(LoggingProperties.PREFIX, LoggingProperties.class)
                .orElseGet(LoggingProperties::new);

        if (isSet(environment, "trace")) {
            logger.info("debug mode, set default threshold to trace");
            properties.getDefaultSpec().setThreshold("trace");
        } else if (isSet(environment, "debug")) {
            logger.info("debug mode, set default threshold to debug");
            properties.getDefaultSpec().setThreshold("debug");
        } else {
            properties.getDefaultSpec().setThreshold("info");
        }

        return properties;
    }

    protected boolean checkProperties(LoggingProperties properties) {
        return properties.isEnabled();
    }

    @SafeVarargs
    public final void root(Level level, Appender<ILoggingEvent>... appenders) {
        ch.qos.logback.classic.Logger logger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (level != null) {
            logger.setLevel(level);
        }
        for (Appender<ILoggingEvent> appender : appenders) {
            logger.addAppender(appender);
        }

        // set console log

        boolean initConsole = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        logger.iteratorForAppenders(), Spliterator.ORDERED), false)
                .noneMatch(a -> a.getName().toLowerCase().contains("console"));

        if (initConsole) {
            Appender<ILoggingEvent> appender = consoleAppender();
            logger.addAppender(appender);
        }
    }

    private Appender<ILoggingEvent> consoleAppender() {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        String logPattern = this.patterns.getProperty("logging.pattern.console", FILE_LOG_PATTERN);
        encoder.setPattern(OptionHelper.substVars(logPattern, context));
        encoder.setCharset(DEFAULT_CHARSET);
        appender.setEncoder(encoder);
        start(encoder);
        appender("console", appender);
        return appender;
    }

    protected Appender<ILoggingEvent> fileAppender(Space space) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        String logPattern = this.patterns.getProperty("logging.pattern.file", FILE_LOG_PATTERN);
        encoder.setPattern(OptionHelper.substVars(logPattern, context));
        encoder.setCharset(DEFAULT_CHARSET);
        appender.setEncoder(encoder);
        start(encoder);

        // parse path and file
        // first consider spec.path, second, default_spec.path, third logging.path
        LogFile logFile = LogFile.get(patterns);
        Properties defaultProperties = new Properties();
        if (logFile != null) {
            logFile.applyTo(defaultProperties);
        }
        String path = space.getSpec().getPath() != null ? space.getSpec().getPath() :
                space.getDefaultSpec().getPath() != null ? space.getDefaultSpec().getPath() :
                        defaultProperties.contains(LoggingSystemProperties.LOG_PATH)
                                ? defaultProperties.getProperty(LoggingSystemProperties.LOG_PATH) :
                                DEFAULT_PATH;
        path = patterns.resolvePlaceholders(path);
        String file = space.getSpec().getFile() != null
                ? fileName(space.getSpec().getFile()) : fileName(space.getName());
        file = patterns.resolvePlaceholders(file);
        appender.setFile(path + "/" + file);
        setRollingPolicy(appender, space, path, file);

        //  threshold config
        ThresholdFilter thresholdFilter = new ThresholdFilter();
        if (space.getSpec().getThreshold() != null) {
            thresholdFilter.setLevel(space.getSpec().getThreshold());
            start(thresholdFilter);
            appender.addFilter(thresholdFilter);
        }

        appender("SPACE-" + space.getName(), appender);
        return appender;
    }

    private void setRollingPolicy(RollingFileAppender<ILoggingEvent> appender, Space space, String path, String file) {
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        String dirName = new File(path, file).getParent();
        rollingPolicy.setFileNamePattern(dirName + "/%d{dd,aux}/" + file + ".%d{yyyy-MM-dd-HH}.%i");
        String maxFileSize = space.getSpec().getMaxFileSize() != null ? space.getSpec().getMaxFileSize() :
                space.getDefaultSpec().getMaxFileSize() != null ? space.getDefaultSpec().getMaxFileSize() :
                        DEFAULT_FILE_SIZE;
        setMaxFileSize(rollingPolicy, maxFileSize);

        // total size cap
        String totalSizeCap = space.getSpec().getTotalSizeCap() != null ? space.getSpec().getTotalSizeCap() :
                space.getDefaultSpec().getTotalSizeCap() != null ? space.getDefaultSpec().getTotalSizeCap() :
                        DEFAULT_TOTAL_SIZE_CAP;

        setTotalSizeCap(rollingPolicy, totalSizeCap);

        int maxHistory = space.getSpec().getMaxHistory() != null ? space.getSpec().getMaxHistory() :
                space.getDefaultSpec().getMaxHistory() != null ? space.getDefaultSpec().getMaxHistory() :
                        DEFAULT_MAX_HISTORY;
        rollingPolicy.setMaxHistory(maxHistory);

        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.setCleanHistoryOnStart(true);
        rollingPolicy.setParent(appender);
//        rollingPolicy.setCleanHistoryOnStart(true);
        start(rollingPolicy);
    }

    private void setTotalSizeCap(SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy,
                                 String totalSizeCap) {
        try {
            rollingPolicy.setContext(context);
            rollingPolicy.setTotalSizeCap(FileSize.valueOf(totalSizeCap));
        } catch (NoSuchMethodError ex) {
            // Logback < 1.1.8 used String configuration
            // copy from setMaxFileSize
            Method method = ReflectionUtils.findMethod(
                    SizeAndTimeBasedRollingPolicy.class, "setTotalSizeCap", String.class);
            ReflectionUtils.invokeMethod(method, rollingPolicy, totalSizeCap);
        }
    }

    private void setMaxFileSize(SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy,
                                String maxFileSize) {
        try {
            rollingPolicy.setMaxFileSize(FileSize.valueOf(maxFileSize));
        } catch (NoSuchMethodError ex) {
            // Logback < 1.1.8 used String configuration
            Method method = ReflectionUtils.findMethod(
                    SizeAndTimeBasedRollingPolicy.class, "setMaxFileSize", String.class);
            ReflectionUtils.invokeMethod(method, rollingPolicy, maxFileSize);
        }
    }

    public void appender(String name, Appender<?> appender) {
        appender.setName(name);
        start(appender);
    }

    protected String fileName(String key) {
        return key.endsWith(".log") ? key : key + ".log";
    }

    public void start(LifeCycle lifeCycle) {
        if (lifeCycle instanceof ContextAware) {
            ((ContextAware) lifeCycle).setContext(context);
        }
        lifeCycle.start();
    }
}
