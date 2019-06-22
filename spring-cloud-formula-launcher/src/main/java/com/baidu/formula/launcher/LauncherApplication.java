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
package com.baidu.formula.launcher;

import com.baidu.formula.launcher.config.LauncherProperties;
import com.baidu.formula.launcher.exception.LaunchingException;
import com.baidu.formula.launcher.impl.DependencyProcessor;
import com.baidu.formula.launcher.impl.FormulaLauncher;
import com.baidu.formula.launcher.reolver.DependencyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.LiveBeansView;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Bowu Dong (tq02ksu@gmail.com) 08/02/2018
 */
@SpringBootApplication
@EnableConfigurationProperties(LauncherProperties.class)
public class LauncherApplication {

    @Autowired
    private LauncherProperties launcherProperties;

    private static String originalMBeanDomain;
    private static String originalAdminEnabled;

    public static void main(String[] args) {
        originalMBeanDomain = System.getProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME);
        originalAdminEnabled = System.getProperty("spring.application.admin.enabled");

        System.clearProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME);
        System.clearProperty("spring.application.admin.enabled");
        SpringApplication.run(LauncherApplication.class, args);
    }

    @ConditionalOnMissingBean
    @Bean
    public DependencyProcessor dependencyProcessor(DependencyResolver dependencyResolver) throws Exception {
        return new DependencyProcessor(launcherProperties, dependencyResolver);
    }

    @Bean
    public CommandLineRunner commandLineRunner(DependencyProcessor dependencyProcessor, ResourceLoader resourceLoader) {
        return args -> {
            if (launcherProperties.getApplicationPath() == null) {
                throw new LaunchingException("application path can not be null");
            }

            if (originalMBeanDomain != null) {
                System.setProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME, originalMBeanDomain);
            }
            if (originalAdminEnabled != null) {
                System.setProperty("spring.application.admin.enabled", originalAdminEnabled);
            }

            Resource gitResource = resourceLoader.getResource("classpath:/git.properties");

            try (InputStream in = gitResource.getInputStream()) {
                Properties props = new Properties();
                props.load(in);

                props.forEach((key, value) -> {
                    System.setProperty("formula.launcher." + key, (String) value);
                });
            } catch (FileNotFoundException e) {
                // do nothing
            }

            new FormulaLauncher(launcherProperties, dependencyProcessor).launch(args);
        };
    }
}
