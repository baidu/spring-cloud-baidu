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

import com.ecwid.consul.v1.ConsulClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author luoguangming
 */
@DirtiesContext
public class ConsulPropertySourceLocatorTests {

    private static final String APP_NAME = "testConsulPropertySourceLocator";

    private static final String PREFIX = "BMS/application/default";

    private static final String VALUE1 = "testPropVal";

    private static final String TEST_PROP = "testProp";

    private static final String KEY1 = PREFIX + "/default/MSC-1/full/" + TEST_PROP;

    private static final String VALUE2 = "testPropVal2";

    private static final String TEST_PROP2 = "testProp2";

    private static final String KEY2 = PREFIX + "/app-test/MSC-1/full/" + TEST_PROP2;

    private ConfigurableApplicationContext context;

    private ConfigurableEnvironment environment;

    private ConsulClient client;

    private ConsulProperties properties;

    @Before
    public void setup() {
        this.properties = new ConsulProperties();
        this.client = new ConsulClient(this.properties.getHost(),
                this.properties.getPort());
        this.client.deleteKVValues(PREFIX);
        this.client.setKVValue(KEY1, VALUE1);
        this.client.setKVValue(KEY2, VALUE2);

        this.context = new SpringApplicationBuilder(Config.class)
                .web(WebApplicationType.NONE).run("--SPRING_APPLICATION_NAME=" + APP_NAME,
                        "--spring.cloud.consul.config.prefix=" + PREFIX,
                        "--spring.cloud.consul.config.token-enabled=false",
                        "--spring.cloud.consul.config.system-labels=app-test,env-test,d-test",
                        "spring.cloud.consul.config.watch.delay=10");

        this.client = this.context.getBean(ConsulClient.class);
        this.properties = this.context.getBean(ConsulProperties.class);
        this.environment = this.context.getEnvironment();
    }

    @After
    public void teardown() {
        this.client.deleteKVValues(PREFIX);
        this.context.close();
    }

    @Test
    public void propertyLoaded() throws Exception {
        String testProp = this.environment.getProperty(TEST_PROP);
        assertThat(testProp).as("testProp was wrong").isEqualTo(VALUE1);
    }

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public CountDownLatch countDownLatch1() {
            return new CountDownLatch(1);
        }

        @Bean
        public CountDownLatch countDownLatch2() {
            return new CountDownLatch(1);
        }

        @EventListener
        public void handle(EnvironmentChangeEvent event) {
            if (event.getKeys().contains(TEST_PROP)) {
                countDownLatch1().countDown();
            } else if (event.getKeys().contains(TEST_PROP2)) {
                countDownLatch2().countDown();
            }
        }

    }

}
