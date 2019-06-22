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
package com.baidu.formula.launcher.model;

import com.baidu.formula.launcher.config.LauncherProperties;
import com.baidu.formula.launcher.reolver.MavenDependencyResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DependenyInfoTest {
    @Autowired
    private LauncherProperties launcherProperties;

    @Autowired
    private MavenDependencyResolver mavenDependencyResolver;

    @Test
    public void contextLoads() {
        Assert.assertNotNull(launcherProperties);
        Assert.assertNotNull(mavenDependencyResolver);
    }

    @Test
    public void generateDependencies() {

        // from maven repository resolve dependencies for properties.substitutions,
        // write to dependency info field, and dump to stdout via snakeyaml api
        launcherProperties.setDependencyInfos(new ArrayList<>());
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.addAll(launcherProperties.getSubstitutions());
        dependencies.addAll(launcherProperties.getIncludes());
        List<Dependency> versions = dependencies.stream()
                .filter(d -> !d.getArtifactId().equals("log4j-over-slf4j"))
                .distinct()
                .flatMap(s ->
                mavenDependencyResolver.resolveVersionRange(s).stream()
        ).collect(Collectors.toList());
        versions.forEach(version -> {
            List<Dependency> deps = mavenDependencyResolver.resolveDependency(version);
            launcherProperties.getDependencyInfos().add(new LauncherProperties.DependencyInfo(version, deps));
        });

        print(launcherProperties.getDependencyInfos());
    }

    /**
     * - artifact: { groupId: com.baidu.formula, artifactId: }
     * classpath:
     * - {}
     * - {}
     *
     * @param dependencyInfos
     */
    private void print(List<LauncherProperties.DependencyInfo> dependencyInfos) {
        dependencyInfos.forEach(info -> {
            System.out.printf("      - artifact: { groupId: %s, artifactId: %s, version: %s }\n",
                    info.getArtifact().getGroupId(), info.getArtifact().getArtifactId(), info.getArtifact().getVersion());
            System.out.println("        dependencies:");
            info.getDependencies().forEach(dep ->
                    System.out.printf("         - { groupId: %s, artifactId: %s, version: %s }\n",
                            dep.getGroupId(), dep.getArtifactId(), dep.getVersion())
            );
        });
    }
}
