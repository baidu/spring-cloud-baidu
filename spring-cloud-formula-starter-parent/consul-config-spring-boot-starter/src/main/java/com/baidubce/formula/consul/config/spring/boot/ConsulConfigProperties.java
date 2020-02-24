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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.style.ToStringCreator;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author luoguangming
 */
@ConfigurationProperties("spring.cloud.consul.config")
@Validated
public class ConsulConfigProperties {

    private boolean enabled = true;

    private String prefix = "config";

    @NotEmpty
    private String defaultContext = "default";

    private String systemLabels;

    private String path = "";

    @NotEmpty
    private String profileSeparator = ",";

    @NotNull
    private Format format = Format.KEY_VALUE;

    /**
     * If format is Format.PROPERTIES or Format.YAML then the following field is used as
     * key to look up consul for configuration.
     */
    @NotEmpty
    private String dataKey = "data";

    /**
     * Uri for remote auth server
     */
    private String authUri;

    /**
     * If the token is enabled for remote server. Defaults to true.
     */
    private Boolean tokenEnabled = true;

    private Watch watch = new Watch();

    /**
     * Throw exceptions during config lookup if true, otherwise, log warnings.
     * change default value to false
     */
    private boolean failFast = false;

    /**
     * Alternative to spring.application.name to use in looking up values in consul KV.
     */
    private String name;

    public ConsulConfigProperties() {
    }

    @PostConstruct
    public void init() {
        if (this.format == Format.FILES) {
            this.profileSeparator = "-";
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDefaultContext() {
        return this.defaultContext;
    }

    public void setDefaultContext(@NotEmpty String defaultContext) {
        this.defaultContext = defaultContext;
    }

    public String getSystemLabels() {
        return this.systemLabels;
    }

    public void setSystemLabels(String systemLabels) {
        this.systemLabels = systemLabels;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProfileSeparator() {
        return this.profileSeparator;
    }

    public void setProfileSeparator(@NotEmpty String profileSeparator) {
        this.profileSeparator = profileSeparator;
    }

    public Format getFormat() {
        return this.format;
    }

    public void setFormat(@NotNull Format format) {
        this.format = format;
    }

    public String getDataKey() {
        return this.dataKey;
    }

    public void setDataKey(@NotEmpty String dataKey) {
        this.dataKey = dataKey;
    }

    public Watch getWatch() {
        return this.watch;
    }

    public void setWatch(Watch watch) {
        this.watch = watch;
    }

    public boolean isFailFast() {
        return this.failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthUri() {
        return authUri;
    }

    public void setAuthUri(String authUri) {
        this.authUri = authUri;
    }

    public Boolean isTokenEnabled() {
        return tokenEnabled;
    }

    public void setTokenEnabled(Boolean tokenEnabled) {
        this.tokenEnabled = tokenEnabled;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("enabled", this.enabled)
                .append("prefix", this.prefix)
                .append("defaultContext", this.defaultContext)
                .append("profileSeparator", this.profileSeparator)
                .append("format", this.format).append("dataKey", this.dataKey)
                .append("watch", this.watch)
                .append("failFast", this.failFast).append("name", this.name).toString();
    }

    /**
     * Consul watch properties.
     */
    public static class Watch {

        /**
         * The number of seconds to wait (or block) for watch query, defaults to 55. Needs
         * to be less than default ConsulClient (defaults to 60). To increase ConsulClient
         * timeout create a ConsulClient bean with a custom ConsulRawClient with a custom
         * HttpClient.
         */
        private int waitTime = 55;

        /**
         * If the watch is enabled. Defaults to true.
         */
        private boolean enabled = true;

        /**
         * The value of the fixed delay for the watch in millis. Defaults to 1000.
         */
        private int delay = 1000;

        public Watch() {
        }

        public int getWaitTime() {
            return this.waitTime;
        }

        public void setWaitTime(int waitTime) {
            this.waitTime = waitTime;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDelay() {
            return this.delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this).append("waitTime", this.waitTime)
                    .append("enabled", this.enabled).append("delay", this.delay)
                    .toString();
        }

    }

}
