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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.interceptor;

import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.util.FormulaConfigUtils;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.util.SystemTag;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luoguangming on 2019/5/23.
 * Interceptor for Feign
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    private String serviceName;

    public FeignRequestInterceptor(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void apply(RequestTemplate template) {
        // add system tags from env
        FormulaConfigUtils.getSystemTags().forEach((k, v) -> {
            template.header(k, v);
        });
        // add service name from input
        template.header(SystemTag.SERVICE_NAME, serviceName);
        logger.debug("Feign: insert {} into httpHeader: {}", SystemTag.SERVICE_NAME, serviceName);
    }
}
