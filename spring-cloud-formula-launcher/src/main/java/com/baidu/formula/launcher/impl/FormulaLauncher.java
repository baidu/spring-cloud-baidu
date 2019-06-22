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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.ExecutableArchiveLauncher;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

/**
 * @author Bowu Dong (tq02ksu@gmail.com) 27/02/2018
 */
public class FormulaLauncher extends ExecutableArchiveLauncher {
    private static final Logger logger = LoggerFactory.getLogger(FormulaLauncher.class);

    private static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";

    static final String BOOT_INF_LIB = "BOOT-INF/lib/";

    private final DependencyProcessor dependencyProcessor;

    public FormulaLauncher(LauncherProperties properties, DependencyProcessor dependencyProcessor) throws Exception {
        super(new JarFileArchive(new File(properties.getApplicationPath())));
        this.dependencyProcessor = dependencyProcessor;
    }

    @Override
    public boolean isNestedArchive(Archive.Entry entry) {
        if (entry.isDirectory()) {
            return entry.getName().equals(BOOT_INF_CLASSES);
        }
        return entry.getName().startsWith(BOOT_INF_LIB);
    }

    @Override
    protected void postProcessClassPathArchives(List<Archive> archives) {
        List<Action> actions = dependencyProcessor.process(archives);

        setSystemProperties(actions, archives);
    }

    private void setSystemProperties(List<Action> actions, List<Archive> archives) {
        // actions
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);

            System.setProperty("formula.launcher.actions[" + i + "].type", action.getType().name());
            System.setProperty("formula.launcher.actions[" + i + "].archive", action.getArchive());
            if (action.getOldArchive() != null) {
                System.setProperty("formula.launcher.actions[" + i + "].old-archive", action.getOldArchive());
            }
        }

        // archives
        for (int i = 0; i < archives.size(); i++) {
            Archive archive = archives.get(i);
            System.setProperty("formula.launcher.archives[" + i + "]", archive.toString());
        }
    }

    @Override
    protected ClassLoader createClassLoader(URL[] urls) {
        logger.info("creating class loader with urls: {}", Arrays.asList(urls));
        return new LaunchedURLClassLoader(urls, deduceParentClassLoader());
    }

    private ClassLoader deduceParentClassLoader() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        ClassLoader context = Thread.currentThread().getContextClassLoader();

        if (cl != context) {
            cl = context.getParent();
        }

        logger.info("deduced Parent Class Loader: {}, urls: {}", cl,
                cl instanceof URLClassLoader ? Arrays.asList(((URLClassLoader) cl).getURLs()) : "-");
        return cl;
    }

    public void launch(String[] args) throws Exception {
        super.launch(args);
    }
}
