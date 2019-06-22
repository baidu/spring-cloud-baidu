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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.transport.wagon.PlexusWagonConfigurator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.transport.wagon.WagonConfigurator;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.OrDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.archive.Archive;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class MavenDependencyResolver implements DependencyResolver {
    private static final Logger logger = LoggerFactory.getLogger(MavenDependencyResolver.class);

    private RepositorySystem repositorySystem;

    private RepositorySystemSession session;

    @PostConstruct
    public void init() {
        this.repositorySystem = newRepositorySystem();
        this.session = newSession(repositorySystem);
    }

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
            return getArtifactsDependencies(project, null);
        } catch (IOException | XmlPullParserException
                | DependencyCollectionException | DependencyResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<com.baidu.formula.launcher.model.Dependency> getArtifactsDependencies(
            MavenProject project, String scope)
            throws DependencyCollectionException, DependencyResolutionException {
        DefaultArtifact pomArtifact = new DefaultArtifact(project.getId());

        List<RemoteRepository> remoteRepos = project.getRemoteProjectRepositories();
        List<Dependency> ret = new ArrayList<Dependency>();

        Dependency dependency = new Dependency(pomArtifact, scope);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(remoteRepos);

        DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot();
        DependencyRequest projectDependencyRequest = new DependencyRequest(node, null);

        repositorySystem.resolveDependencies(session, projectDependencyRequest);

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);

        ret.addAll(nlg.getDependencies(true));
        return ret.stream().map(d -> {
            com.baidu.formula.launcher.model.Dependency dep = new com.baidu.formula.launcher.model.Dependency();
            dep.setArtifactId(d.getArtifact().getArtifactId());
            dep.setGroupId(d.getArtifact().getGroupId());
            dep.setVersion(d.getArtifact().getVersion());
            return dep;
        }).collect(Collectors.toList());
    }

    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(WagonConfigurator.class, PlexusWagonConfigurator.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        String home = System.getenv("HOME");
        LocalRepository localRepo = new LocalRepository(new File(home, ".m2/repository"));
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    public List<com.baidu.formula.launcher.model.Dependency> resolveVersionRange(
            com.baidu.formula.launcher.model.Dependency a) {
        List<Version> versions = getAllVersions(a.getGroupId(), a.getArtifactId());
        return versions.stream().map(v -> com.baidu.formula.launcher.model.Dependency.withId(
                null, a.getGroupId(), a.getArtifactId(), v.toString())).collect(Collectors.toList());
    }

    public List<Version> getAllVersions(String groupId, String artifactId) {
        String repositoryUrl = "http://maven.repo/nexus/content/groups/public";
        RepositorySystem repoSystem = newRepositorySystem();

        RepositorySystemSession session = newSession(repoSystem);
        RemoteRepository central = null;
        central = new RemoteRepository.Builder("central", "default", repositoryUrl).build();

        Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":[0,)");
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.addRepository(central);
        VersionRangeResult rangeResult;
        try {
            rangeResult = repoSystem.resolveVersionRange(session, rangeRequest);
        } catch (VersionRangeResolutionException e) {
            throw new RuntimeException(e);
        }
        List<Version> versions = rangeResult.getVersions().stream()
                .filter(v -> !v.toString().toLowerCase().endsWith("-snapshot"))
                .filter(v -> !groupId.contains("org.springframework") || v.toString().equals("2.0.0.RELEASE"))
                .collect(Collectors.toList());
        logger.info("artifact: {}, Available versions: {}", artifact, versions);
        return versions;
    }

    public List<com.baidu.formula.launcher.model.Dependency> resolveDependency(com.baidu.formula.launcher.model.Dependency d) {
        String coords = String.format("%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion());

        List<Dependency> ret = new ArrayList<Dependency>();

        Dependency dependency = new Dependency(new DefaultArtifact(coords), "compile");
        RemoteRepository repository = new RemoteRepository.Builder(
                "baidu",
                "default",
                "http://maven.repo/nexus/content/groups/public"
        ).build();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.addRepository(repository);

        DependencyNode node;
        try {
            node = repositorySystem.collectDependencies(session, collectRequest).getRoot();
        } catch (DependencyCollectionException e) {
            throw new RuntimeException(e);
        }
        DependencyRequest projectDependencyRequest = new DependencyRequest(node,
                new AndDependencyFilter(new ScopeDependencyFilter("test", "provided"),
                        (node1, parents) -> {
                            Dependency dep = node1.getDependency();
                            if (dep.getOptional() != null && dep.getOptional()) {
                                return false;
                            }

                            return parents.stream()
                                    .map(DependencyNode::getDependency).filter(Objects::nonNull)
                                    .noneMatch(Dependency::getOptional);
                        }));

        try {
            repositorySystem.resolveDependencies(session, projectDependencyRequest);
        } catch (DependencyResolutionException e) {
            throw new RuntimeException(e);
        }

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);

        ret.addAll(nlg.getDependencies(false));
        return ret.stream()
                .filter(dep -> dep.getOptional() == null || !dep.getOptional())
                .filter(dep -> Arrays.asList("compile", "runtime").contains(dep.getScope()))
                .map(Dependency::getArtifact)
                .map(a -> {
                    com.baidu.formula.launcher.model.Dependency dep = new com.baidu.formula.launcher.model.Dependency();
                    dep.setArtifactId(a.getArtifactId());
                    dep.setGroupId(a.getGroupId());
                    dep.setVersion(a.getVersion());
                    return dep;
                }).collect(Collectors.toList());
    }
}
