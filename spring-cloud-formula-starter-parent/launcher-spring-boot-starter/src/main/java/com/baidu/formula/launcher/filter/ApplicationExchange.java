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
package com.baidu.formula.launcher.filter;

import com.baidu.formula.launcher.config.ApplicationConfiguration;

import java.net.URL;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class ApplicationExchange {
    private Integer serverPort;
    private URL[] classPathUrls;
    private String contextPath;
    private ApplicationConfiguration application;

    public Integer getServerPort() {

        return serverPort;
    }

    public URL[] getClassPathUrls() {
        return classPathUrls;
    }

    public void setClassPathUrls(URL[] classPathUrls) {
        this.classPathUrls = classPathUrls;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public ApplicationConfiguration getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfiguration application) {
        this.application = application;
    }
}
