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

import org.springframework.boot.info.GitProperties;
import org.springframework.boot.info.InfoProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class LauncherInfoProperties extends InfoProperties {
    private final Properties entries;

    /**
     * Create an instance with the specified entries.
     *
     * @param entries the information to expose
     */
    public LauncherInfoProperties(Properties entries) {
        super(entries);
        this.entries = entries;
    }

    public GitProperties getGit() {
        Properties e = entries.entrySet()
                .stream()
                .filter(entry -> entry.toString().startsWith("git"))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString().replace("git", ""),
                        Map.Entry::getValue, (v1, v2) -> {
                            throw new RuntimeException();
                        },
                        Properties::new));
        return new GitProperties(e);
    }

    public List<Action> getActions() {
        List<Action> actions = new ArrayList<>();
        for (int i = 0; ; i++) {
            Action action = new Action();
            String type = entries.getProperty("archives." + i + "type");
            if (type == null) {
                break;
            }

            action.setType(ActionType.valueOf(type));

            action.setArchive(entries.getProperty("archives." + i + "archive"));
            action.setOldArchive(entries.getProperty("archives." + i + "old-archive"));
        }

        return actions;
    }

    public List<String> getArchives() {
        return entries.entrySet().stream()
                .filter(entry -> entry.getKey().toString().startsWith("archives"))
                .map(Map.Entry::getValue)
                .map(Object::toString).collect(Collectors.toList());
    }

    public enum ActionType {
        ADD, REMOVE, OVERRIDE
    }

    public class Action {
        private ActionType type;

        private String archive;

        private String oldArchive;

        public ActionType getType() {
            return type;
        }

        public void setType(ActionType type) {
            this.type = type;
        }

        public String getArchive() {
            return archive;
        }

        public void setArchive(String archive) {
            this.archive = archive;
        }

        public String getOldArchive() {
            return oldArchive;
        }

        public void setOldArchive(String oldArchive) {
            this.oldArchive = oldArchive;
        }
    }
}
