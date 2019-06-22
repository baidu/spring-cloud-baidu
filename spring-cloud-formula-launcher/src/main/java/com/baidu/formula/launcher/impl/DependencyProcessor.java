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
package com.baidu.formula.launcher.impl;

import com.baidu.formula.launcher.config.LauncherProperties;
import com.baidu.formula.launcher.model.Action;
import com.baidu.formula.launcher.model.ActionType;
import com.baidu.formula.launcher.model.Dependency;
import com.baidu.formula.launcher.reolver.DependencyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class DependencyProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DependencyProcessor.class);

    private final LauncherProperties properties;

    private final List<Archive> frameworkArchives;

    private final DependencyResolver dependencyResolver;

    public DependencyProcessor(LauncherProperties properties, DependencyResolver dependencyResolver) throws Exception {
        this.properties = properties;
        this.dependencyResolver = dependencyResolver;
        JarFileArchive frameworkArchive = new JarFileArchive(new File(properties.getFrameworkPath()));
        frameworkArchives = frameworkArchive
                .getNestedArchives(entry -> entry.getName().startsWith(FormulaLauncher.BOOT_INF_LIB));
    }

    public List<Action> process(List<Archive> archives) {
        List<Action> actions = new ArrayList<>();

        boolean springBoot15 = archives.stream().anyMatch(ar -> ar.toString().contains("spring-boot-1."));

        if (springBoot15) {
            logger.info("spring boot 1.5 detected, skip for jar replacing, class path urls: {}", archives);
            return new ArrayList<>();
        }

        processIncludes(archives, actions);

        processExclusions(archives, actions);

        processSubstitutions(archives, actions);

        return actions;
    }

    private void processIncludes(List<Archive> archives, List<Action> actions) {
        if (CollectionUtils.isEmpty(properties.getIncludes())) {
            return;
        }

        List<Dependency> includes = properties.getIncludes();

        includes.forEach(include -> {
            boolean replaced = replaceDependency(include, true, archives, actions);

            if (!replaced) {
                return;
            }

            // process dependencies
            Archive includeArchive = findArchive(frameworkArchives, include);
            List<Dependency> deps = findDependencyInfo(include, includeArchive);
            deps.forEach(dep -> replaceDependency(dep, true, archives, actions));
        });
    }

    private void processExclusions(List<Archive> archives, List<Action> actions) {
        if (CollectionUtils.isEmpty(properties.getExclusions())) {
            return;
        }

        List<Dependency> exclusions = properties.getExclusions();
        exclusions.forEach(exclusion -> {
            Archive archive = findArchive(archives, exclusion);

            if (archive == null) {
                return;
            }

            logger.warn("removing illegal artifact: {}", archive);
            archives.remove(archive);
            actions.add(Action.builder().type(ActionType.REMOVE).archive(archive.toString()).build());
        });

        properties.getExclusions().forEach(exclusion -> {
            Archive archive = findArchive(archives, exclusion);
            if (archive != null) {
                logger.info("removing archive: {}", archive);
                archives.remove(archive);
                actions.add(Action.builder().type(ActionType.REMOVE).archive(archive.toString()).build());
            }
        });
    }

    private void processSubstitutions(List<Archive> archives, List<Action> actions) {
        if (CollectionUtils.isEmpty(properties.getSubstitutions())) {
            return;
        }

        List<Dependency> substitutions = properties.getSubstitutions();
        substitutions.forEach(substitution -> {
            boolean replaced = replaceDependency(substitution, false, archives, actions);

            if (!replaced) {
                return;
            }

            // process dependencies
            Archive origin = findArchive(frameworkArchives, substitution);
            List<Dependency> deps = findDependencyInfo(substitution, origin);
            deps.forEach(dep -> replaceDependency(dep, true, archives, actions));
        });
    }

    private boolean replaceDependency(Dependency dependency, boolean forceAdd,
                                      List<Archive> archives, List<Action> actions) {
        Archive archive = findArchive(frameworkArchives, dependency);
        if (archive == null) {
            logger.warn("dependency not found, {}", dependency);
            return false;
        }

        Archive currentArchive = findArchive(archives, dependency);

        if (same(archive, currentArchive) && !dependency.getGroupId().contains("baidu")) {
            return false;
        }

        if (currentArchive == null && !forceAdd) {
            return false;
        }

        logger.info("adding launcher archive: {}", archive);
        archives.add(archive);
        actions.add(Action.builder().type(ActionType.ADD).archive(archive.toString()).build());

        if (currentArchive != null) {
            logger.info("removing launcher archive: {}", currentArchive);
            archives.remove(currentArchive);
        }
        return true;
    }

    private boolean same(Archive includeArchive, Archive currentArchive) {
        if (includeArchive == null || currentArchive == null) {
            return includeArchive == currentArchive;
        }

        String left = includeArchive.toString().replaceFirst("^.*" + FormulaLauncher.BOOT_INF_LIB, "");
        String right = currentArchive.toString().replaceFirst("^.*" + FormulaLauncher.BOOT_INF_LIB, "");

        return left.equals(right);
    }

    Archive findArchive(List<Archive> archives, Dependency dependency) {
        String entryName = "META-INF/maven/" + dependency.getGroupId() + "/" + dependency.getArtifactId() + "/pom.xml";

        for (Archive archive : archives) {
            if (StreamSupport.stream(archive.spliterator(), false).anyMatch(entry -> entry.getName().equals(entryName))) {
                return archive;
            }
        }

        return null;
    }

    private List<Dependency> findDependencyInfo(Dependency dependency, Archive archive) {
        Predicate<LauncherProperties.DependencyInfo> predicate = dependencyInfo ->
                dependency.getArtifactId().equals(dependencyInfo.getArtifact().getArtifactId());

        if (dependency.getVersion() == null) {
            predicate.and(info -> {
                String url = archive.toString();
                return url.contains("/" + info.getArtifact().getArtifactId() + "-" + info.getArtifact().getVersion());
            });
        } else {
            predicate.and(info -> dependency.getVersion().equals(info.getArtifact().getVersion()));
        }
        return properties.getDependencyInfos().stream()
                .filter(predicate)
                .map(LauncherProperties.DependencyInfo::getDependencies).findFirst().orElseGet(() -> {
                    logger.warn("dependencies info not found, return empty collection, dependency={}, archive={}",
                            dependency, archive);
                    return new ArrayList<>();
                });
    }
}
