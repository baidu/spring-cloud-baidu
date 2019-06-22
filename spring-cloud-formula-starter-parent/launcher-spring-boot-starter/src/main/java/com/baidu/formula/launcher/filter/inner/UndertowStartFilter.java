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
package com.baidu.formula.launcher.filter.inner;

import com.baidu.formula.launcher.filter.ApplicationExchange;
import com.baidu.formula.launcher.filter.LauncherFilter;
import com.baidu.formula.launcher.filter.LauncherFilterChain;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.springframework.boot.loader.LaunchedURLClassLoader;

import javax.servlet.ServletException;

/**
 * Bowu Dong (tq02ksu@gmail.com)
 */
public class UndertowStartFilter extends AbstractStartFilterSupport implements LauncherFilter {

    @Override
    public boolean process(ApplicationExchange application, LauncherFilterChain filterChain) {

        int port = application.getServerPort();

        ClassLoader classLoader = new LaunchedURLClassLoader(application.getClassPathUrls(), deduceParentClassLoader());

        DeploymentInfo servletBuilder = Servlets.deployment()
                       .setClassLoader(classLoader)
                       .setContextPath(application.getContextPath())
                       .setDeploymentName(application.getApplication().getPath());

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
 
        manager.deploy();

        Undertow server = null;
        try {
            server = Undertow.builder()
                    .addHttpListener(port, "localhost")
                    .setHandler(manager.start()).build();
            server.start();
        } catch (ServletException e) {
            e.printStackTrace();
        }

        return false;
    }

}
