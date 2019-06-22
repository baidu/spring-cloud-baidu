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
package com.baidu.formula.logging.web;

import com.baidu.formula.logging.web.tomcat.SimpleLogbackValve;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@Configuration
public class WebContainerAccessLogAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Slf4jRequestLog.class)
    @ConditionalOnMissingBean(WebContainerAccessLogger.class)
    static class JettyAutoConfiguration {

        @Bean
        public WebContainerAccessLogger webContainerAccessLogger() {
            return new WebContainerAccessLogger();
        }

        @Bean
        public WebServerFactoryCustomizer accessWebServerFactoryCustomizer() {
            return factory -> {
                if (factory instanceof JettyServletWebServerFactory) {
                    ((JettyServletWebServerFactory) factory).addServerCustomizers((JettyServerCustomizer) server -> {
                        HandlerCollection handlers = new HandlerCollection();
                        for (Handler handler : server.getHandlers()) {
                            handlers.addHandler(handler);
                        }
                        RequestLogHandler reqLogs = new RequestLogHandler();
                        Slf4jRequestLog requestLog = new Slf4jRequestLog();
                        requestLog.setLoggerName("access-log");
                        requestLog.setLogLatency(false);

                        reqLogs.setRequestLog(requestLog);
                        handlers.addHandler(reqLogs);
                        server.setHandler(handlers);
                    });
                }
            };
        }
    }

    /**
     * ref undertow access log javadoc
     * http://undertow.io/javadoc/1.4.x/index.html ctrl+f AccessLogHandler
     */
    @Configuration
    @ConditionalOnClass(Undertow.class)
    @ConditionalOnMissingBean(WebContainerAccessLogger.class)
    static class UndertowAutoConfiguration {

        @Bean
        public WebContainerAccessLogger webContainerAccessLogger() {
            return new WebContainerAccessLogger();
        }

        private AccessLogHandler createAccessLogHandler(HttpHandler handler) {
            Logger logger = LoggerFactory.getLogger("accesslog");
            try {
                /*
                ref nginx log format:
                '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';
                 */
                AccessLogReceiver accessLogReceiver = logger::info;
                String formatString = "%h %l %u [%t] \"%r\" %s %b \"%{i,Referer}\""
                        + " \"%{i,User-Agent}\" \"%{i,X-Forwarded-For}\" %D";
                return new AccessLogHandler(handler, accessLogReceiver, formatString,
                        Undertow.class.getClassLoader());
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create AccessLogHandler", ex);
            }
        }

        @Bean
        public WebServerFactoryCustomizer accesslogWebServerFactoryCustomizer() {
            return factory -> {
                if (factory instanceof UndertowServletWebServerFactory) {

                    HandlerWrapper wrapper = this::createAccessLogHandler;
                    ((UndertowServletWebServerFactory) factory).addBuilderCustomizers(builder ->
                            builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true));
                    ((UndertowServletWebServerFactory) factory).addDeploymentInfoCustomizers(deploymentInfo ->
                            deploymentInfo.addInitialHandlerChainWrapper(wrapper));
                }
            };
        }
    }

    @Configuration
    @ConditionalOnClass(Tomcat.class)
    @ConditionalOnMissingBean(WebContainerAccessLogger.class)
    public class TomcatAutoConfiguration {
        @Bean
        public WebContainerAccessLogger webContainerAccessLogger() {
            return new WebContainerAccessLogger();
        }

        @Bean
        public WebServerFactoryCustomizer accesslogWebServerFactoryCustomizer() {
            return factory -> {
                SimpleLogbackValve valve = new SimpleLogbackValve(LoggerFactory.getLogger("accesslog"));

                ((TomcatServletWebServerFactory) factory).addEngineValves(valve);
            };
        }
    }

    static class WebContainerAccessLogger {
    }
}
