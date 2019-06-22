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
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * Created by cuiweizheng on 19/4/20.
 */
public class AsyncRestTemplateCircuitInterceptor implements AsyncClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRestTemplateCircuitInterceptor.class);

    private CircuitBreakerCore circuitBreakerCore;

    public AsyncRestTemplateCircuitInterceptor(
            CircuitBreakerCore circuitBreakerCore) {
        this.circuitBreakerCore = circuitBreakerCore;
    }

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(HttpRequest httpRequest, byte[] body,
                                                          AsyncClientHttpRequestExecution execution)
            throws IOException {
        logger.debug("AsyncRestTemplateCircuitInterceptor start");
        URI asUri = httpRequest.getURI();
        String httpMethod = httpRequest.getMethod().toString();
        String serviceName = asUri.getHost();
        String url = asUri.getPath();
        logger.info("http with serviceName:{}, menthod:{}, url:{}", serviceName, httpMethod, url);
        if (circuitBreakerCore.checkRulesExist(httpMethod, serviceName, url)) {
            try {
                Method wrappedMethod = AsyncRestTemplateCircuitInterceptor.class.getMethod(
                        "doExecuteAsync",
                        AsyncClientHttpRequestExecution.class,
                        HttpRequest.class, byte[].class);
                Object[] args = {httpRequest, body};
                ListenableFuture<ClientHttpResponse> response =
                        (ListenableFuture<ClientHttpResponse>) circuitBreakerCore.process(httpMethod,
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
            return execution.executeAsync(httpRequest, body);
        }
    }

    public ListenableFuture<ClientHttpResponse> doExecuteAsync(AsyncClientHttpRequestExecution execution,
                                                               HttpRequest httpRequest,
                                                               byte[] body) throws IOException {
        try {
            ListenableFuture<ClientHttpResponse> response = execution.executeAsync(httpRequest, body);
            logger.info("http request response:" + response);
            if (response == null || !HttpStatus.OK.equals(response.get().getStatusCode())) {
                throw new IOException("response error");
            }
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        }

    }
}
