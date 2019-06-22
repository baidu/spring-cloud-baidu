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
package com.baidu.formula.launcher.runner;

import com.baidu.formula.launcher.config.LauncherProperties;
import org.springframework.boot.CommandLineRunner;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class FilterChainRunner implements CommandLineRunner {

    private final LauncherProperties launcherProperties;

    public FilterChainRunner(LauncherProperties launcherProperties) {
        this.launcherProperties = launcherProperties;
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
