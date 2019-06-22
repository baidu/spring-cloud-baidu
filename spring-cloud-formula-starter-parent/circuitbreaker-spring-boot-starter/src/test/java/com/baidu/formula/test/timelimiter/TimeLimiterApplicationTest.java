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
package com.baidu.formula.test.timelimiter;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TimeLimiterApplicationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogicController logicController;

    @Test
    public void testContext() {
        System.out.println(port);
        assertThat(port, Matchers.greaterThan(0));
    }

    @Test
    public void testNormal() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            try {
                mockMvc.perform(
                        get("/free")
                                .param("duration", "0")
                                .param("success-rate", "20")
                ).andExpect(status().isOk());
            } catch (Exception e) {
                // do nothing
            }
        }
        long cost = System.currentTimeMillis() - start;
        assertThat(cost, Matchers.lessThan(1000L));
    }

    @Test
    public void testTimeoutWithException() {
        try {
            logicController.logic(1200);
        } catch (Throwable t) {
            t.printStackTrace();
            fail("error exception");
        }
    }


    @Test
    public void testCircuitBreakerOpen() {
        for (int i = 0; i < 10; i++) {
            try {
                mockMvc.perform(
                        get("/circuit")
                                .param("duration", "0.01")
                                .param("success-rate", "20")
                ).andExpect(status().isOk());
            } catch (Exception e) {
                // do nothing
            }
        }

        // state is open
        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            try {
                mockMvc.perform(
                        get("/circuit")
                                .param("duration", "1")
                                .param("success-rate", "20")
                ).andExpect(status().isOk());
            } catch (Exception e) {
                // do nothing
            }
        }
        long cost = System.currentTimeMillis() - start;
        assertThat(cost, Matchers.lessThan(1000L));
    }
}
