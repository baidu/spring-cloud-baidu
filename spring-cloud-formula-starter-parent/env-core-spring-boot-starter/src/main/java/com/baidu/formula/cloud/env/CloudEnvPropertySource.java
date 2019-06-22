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
package com.baidu.formula.cloud.env;

import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Created by liuruisen on 2019/3/4.
 */
public class CloudEnvPropertySource extends MapPropertySource {

    public CloudEnvPropertySource(String name, Map<String, Object> source) {
        super(name, source);
        // discovery properties
        source.put("formula.discovery.prefer-ip-address", "true");
        source.put("formula.discovery.product-name", System.getenv("EM_PRODUCT_LINE"));
        source.put("formula.discovery.environment", System.getenv("EM_ENV_TYPE"));
        source.put("formula.discovery.tags.0", System.getenv("EM_PLATFORM"));

        // discovery auth info
        source.put("formula.discovery.headers.X-BMS-APP-NAME", System.getenv("EM_APP"));
        source.put("formula.discovery.headers.X-BMS-PAAS-TYPE", System.getenv("EM_PAAS_TYPE"));
        source.put("formula.discovery.headers.X-BMS-PLATFORM-NAME", System.getenv("EM_PLATFORM"));

        // discovery rpcdesc info
        source.put("formula.discovery.customs.appNameAlias", System.getenv("EM_APP"));
        source.put("formula.discovery.customs.platformName", System.getenv("EM_PLATFORM"));
        source.put("formula.discovery.customs.workspaceId", System.getenv("EM_WORKSPACE_ID"));
        source.put("formula.discovery.instanceId", System.getenv("EM_INSTANCE_ID"));


        // config properties
        source.put("spring.cloud.config.name", System.getenv("EM_WORKSPACE_ID") + "-" + getServiceName());
        if (System.getenv("CONFIG_PROFILE") != null) {
            source.put("spring.cloud.config.profile", System.getenv("CONFIG_PROFILE"));
        } else {
            String value = String.format("env-type-%s,%s",
                    System.getenv("EM_ENV_TYPE"), System.getenv("EM_PLATFORM"));
            source.put("spring.cloud.config.profile", value);
        }
        if (System.getenv("CONFIG_VERSION") != null) {
            source.put("spring.cloud.config.label", System.getenv("CONFIG_VERSION"));
        }
        source.put("spring.cloud.config.override-system-properties", "false");

        // config auth properties
        source.put("spring.cloud.config.headers.X-BMS-APP-NAME", System.getenv("EM_APP"));
        source.put("spring.cloud.config.headers.X-BMS-PAAS-TYPE", System.getenv("EM_PAAS_TYPE"));
        source.put("spring.cloud.config.headers.X-BMS-PLATFORM-NAME", System.getenv("EM_PLATFORM"));

        // ribbon refresh cache interval.
        source.put("ribbon.ServerListRefreshInterval", 20000);

    }

    private String getServiceName() {
        String serviceName = "";
        Properties properties = loadBootstrap();
        if (properties != null) {
            serviceName = properties.getProperty("spring.application.name");
            if (serviceName != null) {
                return serviceName;
            }
        }
        properties = loadProperties();
        if (properties != null) {
            serviceName = properties.getProperty("spring.application.name");
        }
        return serviceName;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        InputStream propsStream = this.getClass().getClassLoader().getResourceAsStream("application.properties");
        if (propsStream == null) {
            propsStream = this.getClass().getClassLoader().getResourceAsStream("application.yml");
            if (propsStream == null) {
                return null;
            }
        }
        try {
            props.load(propsStream);
            propsStream.close();
        } catch (IOException e) {
            return null;
        }
        return props;
    }

    private Properties loadBootstrap() {
        Properties props = new Properties();
        InputStream propsStream = this.getClass().getClassLoader().getResourceAsStream("bootstrap.properties");
        if (propsStream == null) {
            propsStream = this.getClass().getClassLoader().getResourceAsStream("bootstrap.yml");
            if (propsStream == null) {
                return null;
            }
        }
        try {
            props.load(propsStream);
            propsStream.close();
        } catch (IOException e) {
            return null;
        }
        return props;
    }
}
