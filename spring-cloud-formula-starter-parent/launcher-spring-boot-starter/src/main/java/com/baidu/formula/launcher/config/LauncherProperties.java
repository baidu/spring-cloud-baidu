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
package com.baidu.formula.launcher.config;

import com.baidu.formula.launcher.config.model.FilterDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

import static com.baidu.formula.launcher.config.LauncherProperties.PREFIX;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@ConfigurationProperties(prefix = PREFIX)
public class LauncherProperties {
    static final String PREFIX = "formula.launcher";

    private ApplicationConfiguration application;

    private List<FilterDefinition> filters;

    private LauncherMeta meta;

}
