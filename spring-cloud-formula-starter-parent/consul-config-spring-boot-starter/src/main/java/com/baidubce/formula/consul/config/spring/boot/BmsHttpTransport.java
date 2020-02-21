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

import com.ecwid.consul.transport.AbstractHttpTransport;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * HTTP client with interceptor
 * This class is thread safe
 *
 * @author luoguangming
 */
public final class BmsHttpTransport extends AbstractHttpTransport {

    static final int DEFAULT_MAX_CONNECTIONS = 1000;
    static final int DEFAULT_MAX_PER_ROUTE_CONNECTIONS = 500;
    static final int DEFAULT_CONNECTION_TIMEOUT = 10000; // 10 sec

    // 10 minutes for read timeout due to blocking queries timeout
    // https://www.consul.io/api/index.html#blocking-queries
    static final int DEFAULT_READ_TIMEOUT = 60000 * 10; // 10 min

    private final HttpClient httpClient;

    public BmsHttpTransport(HttpRequestInterceptor requestInterceptor) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(DEFAULT_MAX_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE_CONNECTIONS);

        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT).
                setConnectionRequestTimeout(DEFAULT_CONNECTION_TIMEOUT).
                setSocketTimeout(DEFAULT_READ_TIMEOUT).
                build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().
                setConnectionManager(connectionManager).
                setDefaultRequestConfig(requestConfig).
                useSystemProperties();

        if (requestInterceptor != null) {
            httpClientBuilder.addInterceptorFirst(requestInterceptor);
        }

        this.httpClient = httpClientBuilder.build();
    }

    public BmsHttpTransport() {
        this(null);
    }

    @Override
    protected HttpClient getHttpClient() {
        return httpClient;
    }
}
