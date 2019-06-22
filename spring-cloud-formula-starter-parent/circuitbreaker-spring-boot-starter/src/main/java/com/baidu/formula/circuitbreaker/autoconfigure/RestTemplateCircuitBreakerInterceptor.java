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
/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.formula.circuitbreaker.autoconfigure;

import com.baidu.formula.circuitbreaker.exception.CircuitBreakerOpenException;
import com.baidu.formula.circuitbreaker.impl.CircuitBreakerCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * Created by cuiweizheng on 19/4/20.
 */
public class RestTemplateCircuitBreakerInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateCircuitBreakerInterceptor.class);

    private CircuitBreakerCore circuitBreakerCore;

    public RestTemplateCircuitBreakerInterceptor(
            CircuitBreakerCore circuitBreakerCore) {
        this.circuitBreakerCore = circuitBreakerCore;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
                                        ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        URI asUri = httpRequest.getURI();
        String httpMethod = httpRequest.getMethod().toString();
        String serviceName = asUri.getHost();
        String url = asUri.getPath();
        logger.info("http with serviceName:{}, menthod:{}, url:{}", serviceName, httpMethod, url);
        if (circuitBreakerCore.checkRulesExist(httpMethod, serviceName, url)) {
            try {
                Method wrappedMethod = RestTemplateCircuitBreakerInterceptor.class.getMethod(
                        "doExecute", ClientHttpRequestExecution.class, HttpRequest.class, byte[].class);
                Object[] args = {clientHttpRequestExecution, httpRequest, bytes};
                ClientHttpResponse response = (ClientHttpResponse) circuitBreakerCore.process(httpMethod,
                        serviceName, url, wrappedMethod, this, args);
                // todo 熔断返回null
                return response;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                if (e instanceof CircuitBreakerOpenException) {
                    throw  new RuntimeException(e.getMessage());
                } else if (e instanceof IOException) {
                    throw  new IOException(e.getMessage());
                } else {
                    throw new RuntimeException(e.getMessage());
                }
            }
        } else {
            return clientHttpRequestExecution.execute(httpRequest, bytes);
        }
    }

    public ClientHttpResponse doExecute(ClientHttpRequestExecution clientHttpRequestExecution,
                                        HttpRequest httpRequest, byte[] bytes) throws IOException {
        try {
            ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, bytes);
            logger.info("http request response:" + response);
            if (response == null || !HttpStatus.OK.equals(response.getStatusCode())) {
                throw new IOException("response error");
            }
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        }
    }
}
