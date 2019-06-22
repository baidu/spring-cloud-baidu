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

import com.baidu.formula.launcher.model.Dependency;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bowu Dong (tq02ksu@gmail.com) 08/02/2018
 */
@ConfigurationProperties(prefix = "formula.launcher")
public class LauncherProperties {
    private String applicationPath;
    private String frameworkPath;

    private List<Dependency> exclusions;

    private List<Dependency> includes;

    private List<Dependency> substitutions;

    private List<DependencyInfo> dependencyInfos = new ArrayList<>();

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getFrameworkPath() {
        return frameworkPath;
    }

    public void setFrameworkPath(String frameworkPath) {
        this.frameworkPath = frameworkPath;
    }

    public List<Dependency> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<Dependency> exclusions) {
        this.exclusions = exclusions;
    }

    public List<Dependency> getIncludes() {
        return includes;
    }

    public void setIncludes(List<Dependency> includes) {
        this.includes = includes;
    }

    public List<Dependency> getSubstitutions() {
        return substitutions;
    }

    public void setSubstitutions(List<Dependency> substitutions) {
        this.substitutions = substitutions;
    }

    public List<DependencyInfo> getDependencyInfos() {
        return dependencyInfos;
    }

    public void setDependencyInfos(List<DependencyInfo> dependencyInfos) {
        this.dependencyInfos = dependencyInfos;
    }

    public static class DependencyInfo {
        private Dependency artifact;
        private List<Dependency> dependencies;

        public DependencyInfo() {
        }

        public DependencyInfo(Dependency artifact, List<Dependency> dependencies) {
            this.artifact = artifact;
            this.dependencies = dependencies;
        }

        public Dependency getArtifact() {
            return artifact;
        }

        public void setArtifact(Dependency artifact) {
            this.artifact = artifact;
        }

        public List<Dependency> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<Dependency> dependencies) {
            this.dependencies = dependencies;
        }
    }
}
