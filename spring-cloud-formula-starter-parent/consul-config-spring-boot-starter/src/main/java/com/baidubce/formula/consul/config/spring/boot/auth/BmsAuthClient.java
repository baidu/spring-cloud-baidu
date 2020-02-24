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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

/**
 * @author luoguangming
 */
public class BmsAuthClient {

    private static final Logger logger = LoggerFactory.getLogger(BmsAuthClient.class);

    private static final int timeout = 10; // in second

    private RestTemplate restTemplate;

    private String token = null;

    private String expirationTime;

    public BmsAuthClient() {
        this.restTemplate = getRestTemplate();
    }

    private RestTemplate getRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setReadTimeout(timeout * 1000);
        RestTemplate template = new RestTemplate(requestFactory);

        template.setInterceptors(Collections.singletonList(new BmsAuthorizationInterceptor()));

        return template;
    }

    public String getTokenFromServer(String authUri) {
        if (StringUtils.isEmpty(authUri)) {
            logger.error("Failed to get token: authUri is empty!");
            return null;
        }
        logger.info("Try to get auth token from: " + authUri);
        ResponseEntity<TokenResponse> response = this.restTemplate.getForEntity(authUri, TokenResponse.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            TokenResponse tokenResponse = response.getBody();
            logger.info("Response from auth server is: " + tokenResponse);
            this.token = tokenResponse.getToken();
            this.expirationTime = tokenResponse.getExpirationTime();
            return this.token;
        } else {
            logger.error("Failed to get token from auth server");
            return null;
        }
    }

    public Boolean isTokenExpired() throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date expireDate = df.parse(this.expirationTime);
        return expireDate.before(new Date());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(String expirationTime) {
        this.expirationTime = expirationTime;
    }
}
