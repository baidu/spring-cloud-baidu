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
package com.baidu.formula.logging.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class Space {
    private String name;
    private Spec spec;
    private Spec defaultSpec;
    private Boolean enabled = true;
    private List<String> loggers = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Spec getSpec() {
        return spec;
    }

    public void setSpec(Spec spec) {
        this.spec = spec;
    }

    public Spec getDefaultSpec() {
        return defaultSpec;
    }

    public void setDefaultSpec(Spec defaultSpec) {
        this.defaultSpec = defaultSpec;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getLoggers() {
        return loggers;
    }

    public void setLoggers(List<String> loggers) {
        this.loggers = loggers;
    }
}
