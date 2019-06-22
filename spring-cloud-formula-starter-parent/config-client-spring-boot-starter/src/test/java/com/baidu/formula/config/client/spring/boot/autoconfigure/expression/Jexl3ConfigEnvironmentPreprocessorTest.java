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
package com.baidu.formula.config.client.spring.boot.autoconfigure.expression;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class Jexl3ConfigEnvironmentPreprocessorTest {
    @Test
    public void testCache() {
        JexlEngine engine = new JexlBuilder().cache(100).create();

        JexlExpression expr = engine.createExpression("a = 3 ? 1 : 3");
        JexlExpression expr2 = engine.createExpression("a = 3 ? 1 : 3");
        assertSame(expr, expr2);
    }
}
