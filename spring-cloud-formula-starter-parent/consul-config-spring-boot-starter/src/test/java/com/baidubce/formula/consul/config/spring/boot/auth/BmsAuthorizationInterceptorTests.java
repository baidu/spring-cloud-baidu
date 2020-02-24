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
package com.baidubce.formula.consul.config.spring.boot.auth;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author luoguangming
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BmsAuthorizationInterceptorTests {


    @InjectMocks
    private BmsAuthorizationInterceptor bmsAuthorizationInterceptor;

    @Mock
    private ClientHttpRequestExecution execution;

    @Test
    public void testConstructor() {
        BmsAuthorizationInterceptor interceptor_1 = new BmsAuthorizationInterceptor(null, null, null);
        Assert.assertNotNull(interceptor_1);
        BmsAuthorizationInterceptor interceptor_2 = new BmsAuthorizationInterceptor("test", "test", "test");
        Assert.assertNotNull(interceptor_2);

    }

    @Test
    public void testIntercept() throws IOException {
        when(execution.execute(any(), any())).thenReturn(getResponse(HttpStatus.OK));
        bmsAuthorizationInterceptor.intercept(getRequest(), new byte[]{}, execution);
    }

    private HttpRequest getRequest() {
        HttpRequest request = new HttpRequest() {
            @Override
            public String getMethodValue() {
                return "GET";
            }

            @Override
            public URI getURI() {
                try {
                    String host = "localhost:8500";
                    String path = "/v1/kv";
                    URI asUri = new URI(null, host, path, null);
                    return asUri;
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
        return request;
    }

    private ClientHttpResponse getResponse(HttpStatus status) {
        ClientHttpResponse response = new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return status;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return 0;
            }

            @Override
            public String getStatusText() throws IOException {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                return null;
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }
        };
        return response;
    }
}
