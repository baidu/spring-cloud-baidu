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
package com.baidu.formula.circuitbreaker.autoconfigure.feign;

import com.baidu.formula.circuitbreaker.exception.CircuitBreakerOpenException;
import com.baidu.formula.circuitbreaker.impl.CircuitBreakerCore;
import feign.Client;
import feign.Request;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * 对feign请求进行包装，插入熔断功能
 *
 * @author guobolin
 */
public class FeignClientWrapper implements Client {

    private static final Logger logger = LoggerFactory.getLogger(FeignClientWrapper.class);

    private Client client;

    private CircuitBreakerCore circuitBreakerCore;

    public FeignClientWrapper(Client client, CircuitBreakerCore circuitBreakerCore) {
        this.client = client;
        this.circuitBreakerCore = circuitBreakerCore;

    }


    public Response execute(Request request, Request.Options options) throws IOException {
        String method = request.method();
        URI asUri = URI.create(request.url());
        String serviceName = asUri.getHost();
        String url = asUri.getPath();
        logger.info("wrapper Client,serviceName:{},menthod:{},url:{}", serviceName, method, url);
        if (circuitBreakerCore.checkRulesExist(method, serviceName, url)) {
            try {
                Method wrappedMethod = FeignClientWrapper.class.getMethod("doExecute",
                        Client.class, Request.class, Request.Options.class);
                Object[] wrappedArgs = {client, request, options};
                Response response = (Response) circuitBreakerCore.process(method,
                        serviceName, url, wrappedMethod, this, wrappedArgs);
                // todo 返回值为null
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
            return client.execute(request, options);
        }
    }

    public Response doExecute(Client client, Request request, Request.Options options) throws IOException {
        try {
            Response response = client.execute(request, options);
            if (response == null || response.status() != 200) {
                throw new IOException("response error");
            }
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e.getMessage());
        }
    }
}
