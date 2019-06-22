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
package com.baidu.formula.logging.logback.disabled;

import ch.qos.logback.classic.LoggerContext;
import com.baidu.formula.logging.logback.SpacedLogbackSystem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "debug=true",
        "logging.level.com.baidu=DEBUG",
        "logging.level.com.baidu.ebiz=WARN",
        "formula.logging.enabled=false"
})
public class DisabledFormulaLoggingApplicationTests {
    @Test
    public void contextLoads() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        String val = context.getProperty(SpacedLogbackSystem.KEY_ENABLED);
        Assert.assertEquals("false", val);
        LoggerFactory.getLogger("com.baidu").info("info for root");
    }
}
