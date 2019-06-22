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
package com.baidu.formula.circuitbreaker.autoconfigure.feign;

import com.baidu.formula.circuitbreaker.impl.CircuitBreakerCore;
import feign.Client;
import feign.Feign;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * 支持Resilience4jFeign
 */
public final class Resilience4jFeign {

    public static Builder builder(CircuitBreakerCore circuitBreakerCore) {
        return new Builder(circuitBreakerCore);
    }

    public static final class Builder extends Feign.Builder {

        CircuitBreakerCore circuitBreakerCore;

        // wrapper之前的
        private Client client = new feign.Client.Default((SSLSocketFactory) null, (HostnameVerifier) null);

        public Builder(CircuitBreakerCore circuitBreakerCore) {
            this.circuitBreakerCore = circuitBreakerCore;
            this.client(new FeignClientWrapper(client, circuitBreakerCore));
        }

        /**
         * Will throw an {@link UnsupportedOperationException} exception.
         */
        @Override
        public Feign.Builder client(Client client) {
            if (client == null) {
                return this;
            }

            if (client.equals(this.client)) {
                return this;
            }

            Client target = null;
            if (client instanceof FeignClientWrapper) {
                target = client;
            } else {
                target = new FeignClientWrapper(client, circuitBreakerCore);
            }

            super.client(target);

            return this;
        }

        @Override
        public Feign build() {
            return super.build();
        }

    }
}
