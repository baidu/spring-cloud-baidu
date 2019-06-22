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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class LoggingProperties {
    public static final String PREFIX = "formula.logging";

    private boolean enabled = true;

    private boolean printStatus = false;

    private Spec defaultSpec = new Spec();

    private Map<String, Space> spaces = new HashMap<>();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Spec getDefaultSpec() {
        return defaultSpec;
    }

    public void setDefaultSpec(Spec defaultSpec) {
        this.defaultSpec = defaultSpec;
    }

    public Map<String, Space> getSpaces() {
        return spaces;
    }

    public void setSpaces(Map<String, Space> spaces) {
        this.spaces = spaces;
    }

    public boolean isPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(boolean printStatus) {
        this.printStatus = printStatus;
    }
}
