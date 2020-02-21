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

/**
 * There are many ways in which we can specify configuration in consul i.e.,
 *
 * <ol>
 * <li>Nested key value style: Where value is either a constant or part of the key
 * (nested). For e.g., For following configuration a.b.c=something a.b.d=something
 * else One can specify the configuration in consul with key as
 * "../kv/config/application/a/b/c" and value as "something" and key as
 * "../kv/config/application/a/b/d" and value as "something else"</li>
 * <li>Entire contents of properties file as value For e.g., For following
 * configuration a.b.c=something a.b.d=something else One can specify the
 * configuration in consul with key as "../kv/config/application/properties" and value
 * as whole configuration " a.b.c=something a.b.d=something else "</li>
 * <li>as Json or YML. You get it.</li>
 * </ol>
 * <p>
 * This enum specifies the different Formats/styles supported for loading the
 * configuration.
 *
 * @author luoguangming
 */
public enum Format {

    /**
     * Indicates that the configuration specified in consul is of type native key
     * values.
     */
    KEY_VALUE,

    /**
     * Indicates that the configuration specified in consul is of property style i.e.,
     * value of the consul key would be a list of key=value pairs separated by new
     * lines.
     */
    PROPERTIES,

    /**
     * Indicates that the configuration specified in consul is of YAML style i.e.,
     * value of the consul key would be YAML format.
     */
    YAML,

    /**
     * Indicates that the configuration specified in consul uses keys as files. This
     * is useful for tools like git2consul.
     */
    FILES,

}

