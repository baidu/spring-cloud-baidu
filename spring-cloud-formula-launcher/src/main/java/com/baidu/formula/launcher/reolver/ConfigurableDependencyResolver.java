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
package com.baidu.formula.launcher.reolver;

import com.baidu.formula.launcher.config.LauncherProperties;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@Component
public class ConfigurableDependencyResolver implements DependencyResolver {

    @Autowired
    private LauncherProperties properties;

    public List<com.baidu.formula.launcher.model.Dependency> resolveDependency(Archive origin) {
        Archive.Entry entry = StreamSupport.stream(origin.spliterator(), false)
                .filter(i -> i.getName().startsWith("/META-INF"))
                .filter(i -> i.getName().endsWith("pom.xml"))
                .findFirst().orElse(null);

        if (entry == null) {
            return null;
        }
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        try {
            Object content = origin.getUrl().getContent();
            Model model = mavenReader.read((InputStream) content);
            MavenProject project = new MavenProject(model);
            DefaultArtifact pomArtifact = new DefaultArtifact(project.getId());
            return properties.getDependencyInfos().stream()
                    .filter(info -> info.getArtifact().getArtifactId().equals(pomArtifact.getArtifactId()))
                    .filter(info -> info.getArtifact().getGroupId().equals(pomArtifact.getGroupId()))
                    .map(LauncherProperties.DependencyInfo::getDependencies)
                    .findAny().orElse(null);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }
}
