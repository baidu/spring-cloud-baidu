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
package com.baidu.formula.actuator.info.launcher;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoPropertiesInfoContributor;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class LauncherInfoContributor extends InfoPropertiesInfoContributor<LauncherInfoProperties> {

    public LauncherInfoContributor(LauncherInfoProperties properties, Mode mode) {
        super(properties, mode);
    }

    @Override
    protected PropertySource<?> toSimplePropertySource() {
        Properties props = new Properties();

        AtomicInteger index = new AtomicInteger(0);
        getProperties().getArchives().forEach(archive -> {
            props.setProperty("archives[" + index.get() + "]", archive);
            index.getAndIncrement();
        });

        copyIfSet(props, "git.branch");
        String commitId = getProperties().getGit().getShortCommitId();
        if (commitId != null) {
            props.put("git.commit.id", commitId);
        }
        copyIfSet(props, "git.commit.time");
        return new PropertiesPropertySource("launcher", props);
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("launcher", this.generateContent());
    }
}
