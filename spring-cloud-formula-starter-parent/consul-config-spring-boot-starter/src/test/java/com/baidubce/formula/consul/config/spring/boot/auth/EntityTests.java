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

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;
import org.meanbean.test.BeanTester;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author luoguangming
 */
public class EntityTests {

    @Test
    public void testTokenResponse() {
        BeanTester beanTester = new BeanTester();
        beanTester.testBean(TokenResponse.class);
    }

    @Test
    public void testBmsAuthClient() throws ParseException {
        BeanTester beanTester = new BeanTester();
        beanTester.testBean(BmsAuthClient.class);

        BmsAuthClient bmsAuthClient = new BmsAuthClient();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        bmsAuthClient.setExpirationTime(df.format(new Date()));
        Assert.assertTrue(bmsAuthClient.isTokenExpired());
    }

    @Test
    public void testEqualsAndHashcode() {
        EqualsVerifier.forClass(TokenResponse.class)
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }
}
