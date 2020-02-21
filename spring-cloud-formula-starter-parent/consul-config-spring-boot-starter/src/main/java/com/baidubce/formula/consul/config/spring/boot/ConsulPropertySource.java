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
import com.ecwid.consul.v1.ConsistencyMode;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author luoguangming
 */
public class ConsulPropertySource extends EnumerablePropertySource<ConsulClient> {

    private static final Logger logger = LoggerFactory.getLogger(ConsulPropertySource.class);

    public static final String SPLITTER = ";";

    private final Map<String, Object> properties = new LinkedHashMap<>();

    private String context;

    private ConsulConfigProperties configProperties;

    private Long initialIndex;

    private BmsAuthClient bmsAuthClient;

    public ConsulPropertySource(String context, ConsulClient source, BmsAuthClient bmsAuthClient,
                                ConsulConfigProperties configProperties) {
        super(context, source);
        this.context = context;
        this.configProperties = configProperties;
        this.bmsAuthClient = bmsAuthClient;
    }

    public void init() {
        if (!this.context.endsWith("/")) {
            this.context = this.context + "/";
        }

        if (configProperties.isTokenEnabled() && StringUtils.isEmpty(bmsAuthClient.getToken())) {
            bmsAuthClient.getTokenFromServer(configProperties.getAuthUri());
        }

        logger.info("Try to get KV from consul for context: " + this.context);
        Response<List<GetValue>> response = this.source.getKVValues(this.context,
                this.bmsAuthClient.getToken(), new QueryParams(ConsistencyMode.STALE));

        this.initialIndex = response.getConsulIndex();

        final List<GetValue> values = response.getValue();
        Format format = this.configProperties.getFormat();
        switch (format) {
            case KEY_VALUE:
                parsePropertiesInKeyValueFormat(values);
                logger.info("Properties for context " + this.context + "is ");
                for (Map.Entry<String, Object> entry : this.properties.entrySet()) {
                    logger.info(entry.getKey() + ": " + entry.getValue().toString());
                }
                break;
            case PROPERTIES:
                break;
            case YAML:
                parsePropertiesWithNonKeyValueFormat(values, format);
                break;
            default:
                break;
        }
    }

    public Long getInitialIndex() {
        return this.initialIndex;
    }

    /**
     * Parses the properties in key value style i.e., values are expected to be either a
     * sub key or a constant.
     *
     * @param values values to parse
     */
    protected void parsePropertiesInKeyValueFormat(List<GetValue> values) {
        if (values == null) {
            return;
        }
        Map<String, Map<String, String>> groups = new HashMap<>();
        for (GetValue getValue : values) {
            String key = getValue.getKey();
            // skip all the path which end with /, only process those real keys
            if (!StringUtils.endsWithIgnoreCase(key, "/")) {
                key = key.replace(this.context, "");
                // until now, key = groupId/status/realKey
                String value = getValue.getDecodedValue();
                logger.debug("context：" + this.context + " key is " + key + ", value is " + value);
                if (!StringUtils.isEmpty(key)) {
                    kvGrouping(groups, key, value);
                }
            }
        }
        // process all effective keys
        for (Map.Entry<String, Map<String, String>> entry : groups.entrySet()) {
            String prefix = "full/";
            // check if this group contains grayRelease and if this instance matches
            if (entry.getValue().containsKey("instance/list")) {
                String list = entry.getValue().get("instance/list");
                List idList = Arrays.asList(list.split(SPLITTER));
                String id = System.getenv("EM_INSTANCE_ID");
                if (idList.contains(id)) {
                    prefix = "gray/";
                }
            }

            // based on the status/matching, filter out those keys with the same prefix
            for (Map.Entry<String, String> subGroups : entry.getValue().entrySet()) {
                if (subGroups.getKey().equals(prefix + "kvs")) {
                    String kvs = subGroups.getValue();
                    if (null == kvs) {
                        logger.warn("Null value is ignored for kvs!");
                        return;
                    }
                    // decode kv pairs from this value
                    decodeKVs(kvs);
                }
            }
        }

    }

    private void decodeKVs(String kvs) {
        String[] kvArray = kvs.split(SPLITTER);
        if (kvArray != null && kvArray.length >= 1) {
            for (String kv : kvArray) {
                // split into 2 elements, value may contain '='
                String[] keyValue = kv.split("=", 2);
                if (keyValue != null && keyValue.length == 2) {
                    this.properties.put(keyValue[0], keyValue[1]);
                } else {
                    logger.error("KV pair must contain '=' : " + kv);
                }
            }
        } else {
            logger.error("Value cannot only contain ';'");
        }
    }

    // grouping key/value based on config group ID
    private void kvGrouping(Map<String, Map<String, String>> groups, String key, String value) {
        String[] keySplit = key.split("/", 2);
        if (keySplit != null && keySplit.length == 2) {
            // 1st element = groupId, 2nd element = status/realKey
            // next, put 2nd element within the same groupId into the same map
            if (groups.containsKey(keySplit[0])) {
                groups.get(keySplit[0]).put(keySplit[1], value);
            } else {
                Map<String, String> newGroup = new HashMap<>();
                newGroup.put(keySplit[1], value);
                groups.put(keySplit[0], newGroup);
            }
        }
    }

    /**
     * Parses the properties using the format which is not a key value style i.e., either
     * java properties style or YAML style.
     *
     * @param values values to parse
     * @param format format in which the values should be parsed
     */
    protected void parsePropertiesWithNonKeyValueFormat(List<GetValue> values,
                                                        Format format) {
        if (values == null) {
            return;
        }

        for (GetValue getValue : values) {
            String key = getValue.getKey().replace(this.context, "");
            if (this.configProperties.getDataKey().equals(key)) {
                parseValue(getValue, format);
            }
        }
    }

    protected void parseValue(GetValue getValue, Format format) {
        String value = getValue.getDecodedValue();
        if (value == null) {
            return;
        }

        Properties props = generateProperties(value, format);

        for (Map.Entry entry : props.entrySet()) {
            this.properties.put(entry.getKey().toString(), entry.getValue());
        }
    }

    protected Properties generateProperties(String value,
                                            Format format) {
        final Properties props = new Properties();

        if (format == Format.PROPERTIES) {
            try {
                // Must use the ISO-8859-1 encoding because Properties.load(stream)
                // expects it.
                props.load(new ByteArrayInputStream(value.getBytes(StandardCharsets.ISO_8859_1)));
            } catch (IOException e) {
                logger.error("Can't be encoded with exception: ", e);
                throw new IllegalArgumentException(
                        value + " can't be encoded using ISO-8859-1");
            }
            return props;
        } else if (format == Format.YAML) {
            final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8)));
            return yaml.getObject();
        } else {
            return props;
        }
    }

    protected Map<String, Object> getProperties() {
        return this.properties;
    }

    protected ConsulConfigProperties getConfigProperties() {
        return this.configProperties;
    }

    protected String getContext() {
        return this.context;
    }

    @Override
    public Object getProperty(String name) {
        return this.properties.get(name);
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> strings = this.properties.keySet();
        return strings.toArray(new String[strings.size()]);
    }

}
