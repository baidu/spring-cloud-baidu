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
package com.baidubce.formula.consul.config.spring.boot;

import com.baidubce.formula.consul.config.spring.boot.auth.BmsAuthClient;
import com.ecwid.consul.transport.TLSConfig;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import org.apache.http.HttpRequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * @author luoguangming
 */
@Configuration
@ConditionalOnConsulEnabled
public class ConsulConfigBootstrapConfiguration {

    @Configuration
    @EnableConfigurationProperties
    @Import(ConsulAutoConfiguration.class)
    @ConditionalOnProperty(name = "spring.cloud.consul.config.enabled",
            matchIfMissing = true)
    protected static class ConsulPropertySourceConfiguration {

        private static final Logger logger = LoggerFactory.getLogger(ConsulConfigBootstrapConfiguration.class);

        @Autowired
        private ConsulClient consulClient;

        @Autowired
        private BmsAuthClient bmsAuthClient;

        @Bean
        @ConditionalOnMissingBean
        public ConsulConfigProperties consulConfigProperties() {
            return new ConsulConfigProperties();
        }

        @Bean
        @ConditionalOnMissingBean
        public BmsAuthClient bmsAuthClient() {
            return new BmsAuthClient();
        }

        @Bean
        @Primary
        public ConsulClient consulClient(ConsulProperties consulProperties,
                                         ConsulConfigProperties consulConfigProperties) {
            final int agentPort = consulProperties.getPort();
            final String agentHost = !StringUtils.isEmpty(consulProperties.getScheme())
                    ? consulProperties.getScheme() + "://" + consulProperties.getHost()
                    : consulProperties.getHost();

            logger.info("Init consul host: " + agentHost + " port: " + agentPort);
            if (consulProperties.getTls() != null) {
                ConsulProperties.TLSConfig tls = consulProperties.getTls();
                TLSConfig tlsConfig = new TLSConfig(
                        tls.getKeyStoreInstanceType(),
                        tls.getCertificatePath(),
                        tls.getCertificatePassword(),
                        tls.getKeyStorePath(),
                        tls.getKeyStorePassword()
                );
                return new ConsulClient(agentHost, agentPort, tlsConfig);
            }
            HttpRequestInterceptor httpRequestInterceptor = new BmsCommonInterceptor();
            BmsHttpTransport httpTransport = new BmsHttpTransport(httpRequestInterceptor);
            ConsulRawClient rawClient = new ConsulRawClient(httpTransport.getHttpClient(),
                    agentHost, agentPort, consulConfigProperties.getPath());
            return new ConsulClient(rawClient);
        }

        @Bean
        @ConditionalOnBean(ConsulClient.class)
        public ConsulPropertySourceLocator consulPropertySourceLocator(
                ConsulConfigProperties consulConfigProperties) {
            return new ConsulPropertySourceLocator(this.consulClient, this.bmsAuthClient, consulConfigProperties);
        }

    }

}
