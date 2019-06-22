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
package com.baidu.formula.config.client.spring.boot.autoconfigure;

import org.springframework.core.Ordered;

/**
 * @author Bowu Dong (tq02ksu@gmail.com) 14/01/2018
 */
public class ModuleConfiguration implements Ordered{
    private String applicationName;

    private int order;

    public ModuleConfiguration(String applicationName, int order) {
        this.applicationName = applicationName;
        this.order = order;
    }

    @Override
    public String toString() {
        return "ModuleConfiguration{" + "applicationName='" + applicationName + '\'' + ", order=" + order + '}';
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
