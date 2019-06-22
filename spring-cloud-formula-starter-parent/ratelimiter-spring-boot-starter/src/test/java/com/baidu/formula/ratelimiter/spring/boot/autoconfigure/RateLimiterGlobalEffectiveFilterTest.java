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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure;

import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.ratelimiter.RateLimiterManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


/**
 * Created by luoguangming on 2019/5/9.
 * Test for RateLimiterGlobalEffectiveFilter
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RateLimiterGlobalEffectiveFilterTest {

    private Logger logger = LoggerFactory.getLogger(RateLimiterGlobalEffectiveFilterTest.class);

    @Autowired
    RateLimiterGlobalEffectiveFilter rateLimiterGlobalEffectiveFilter;

    @Autowired
    RateLimiterManager registryManager;

    MockHttpServletResponse response;

    static final String URL_TO_SKIP = "/favicon.ico";
    static final String URL_TO_LIMIT = "/echo/goodbye";

    @Before
    public void init() {
        logger.info("getRateLimiterConfigs: {}", registryManager.getRatelimiterConfigs().toString());
        this.response = new MockHttpServletResponse();
        this.response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void testLimitUrl() throws ServletException, IOException {
        MockHttpServletRequest request = get(URL_TO_LIMIT).accept(MediaType.ALL).buildRequest(new MockServletContext());
        rateLimiterGlobalEffectiveFilter.doFilter(request, this.response, new MockFilterChain());
        assertEquals(200, this.response.getStatus());
        rateLimiterGlobalEffectiveFilter.doFilter(request, this.response, new MockFilterChain());
        assertEquals(200, this.response.getStatus());
        rateLimiterGlobalEffectiveFilter.doFilter(request, this.response, new MockFilterChain());
        rateLimiterGlobalEffectiveFilter.doFilter(request, this.response, new MockFilterChain());
        assertEquals(429, this.response.getStatus());
    }

    @Test
    public void testSkipUrl() throws ServletException, IOException {
        MockHttpServletRequest request = get(URL_TO_SKIP).accept(MediaType.ALL).buildRequest(new MockServletContext());
        rateLimiterGlobalEffectiveFilter.doFilter(request, this.response, new MockFilterChain());
        assertEquals(200, this.response.getStatus());
    }
}
