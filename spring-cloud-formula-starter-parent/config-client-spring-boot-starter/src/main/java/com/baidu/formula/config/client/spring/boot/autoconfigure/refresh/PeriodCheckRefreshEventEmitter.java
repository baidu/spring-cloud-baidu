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

import com.baidu.formula.config.client.spring.boot.autoconfigure.MultiModuleConfigServicePropertySourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class PeriodCheckRefreshEventEmitter extends RefreshEventEmitter implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PeriodCheckRefreshEventEmitter.class);

    @Value("${spring.cloud.config.refresh.period:30}")
    private int period;

    private volatile boolean running;

    private Thread thread;

    public PeriodCheckRefreshEventEmitter(MultiModuleConfigServicePropertySourceLocator locator,
                                          ContextRefresher refresher, Environment environment) {
        super(locator, refresher, environment);

        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("Config-Client-Period-Checker");
    }

    @PostConstruct
    public void init() {
        running = true;
        thread.start();
    }

    @Override
    public void run() {
        MultiModuleConfigServicePropertySourceLocator locator =
                (MultiModuleConfigServicePropertySourceLocator) getLocator();

        while (running) {
            Map<String, String> versions = locator.getVersions();
            logger.debug("Checking Config for Refreshing environment, versions={} ...", versions);
            locator.locate(getEnvironment());

            doCheck(locator.getVersions(), versions);

            try {
                Thread.sleep(1000L * period);
            } catch (InterruptedException e) {
                logger.info("interrupted while sleep", e);
            }
        }
    }

    private void doCheck(Map<String, String> current, Map<String, String> versions) {
        Consumer<Set<String>> logging = keys ->
                logger.info("current and versions different, emitted a refresh event, "
                        + "keys:{}, current={}, versions={}", keys, current, versions);
        if (current.size() != versions.size()) {
            Set<String> keys = getRefresher().refreshEnvironment();
            logging.accept(keys);
            return;
        }

        versions.forEach((key, value) -> {
            if (!current.containsKey(key) || !value.equalsIgnoreCase(current.get(key))) {
                Set<String> keys = getRefresher().refreshEnvironment();
                logging.accept(keys);
            }
        });
    }

    @PreDestroy
    public void destroy() {
        running = false;
        thread.interrupt();
    }
}
