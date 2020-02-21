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

import org.junit.Assert;
import org.junit.Test;
import org.meanbean.test.BeanTester;

/**
 * @author luoguangming
 */
public class ConsulConfigPropertiesTests {

    @Test
    public void testGetAndSet() {
        BeanTester beanTester = new BeanTester();
        beanTester.testBean(ConsulConfigProperties.class);
    }

    @Test
    public void testIsEnable() {
        ConsulConfigProperties consulConfigProperties = new ConsulConfigProperties();
        consulConfigProperties.setEnabled(false);
        Assert.assertFalse(consulConfigProperties.isEnabled());
        consulConfigProperties.setEnabled(true);
        Assert.assertTrue(consulConfigProperties.isEnabled());
    }
}
