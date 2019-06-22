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
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.stream.Stream;

/**
 *     // Create or retrieve an engine
 *     JexlEngine jexl = new JexlBuilder().create();
 *
 *     // Create an expression
 *     String jexlExp = "foo.innerFoo.bar()";
 *     JexlExpression e = jexl.createExpression( jexlExp );
 *
 *     // Create a context and add data
 *     JexlContext jc = new MapContext();
 *     jc.set("foo", new Foo() );
 *
 *     // Now evaluate the expression, getting the result
 *     Object o = e.evaluate(jc);
 *
 * Support for invocation of any accessible method (see example above).
 * Support for setting/getting any accessible public field.
 * A general new() method allowing to instantiate objects.
 * A general size() method, which works on:
 * String - returns length
 * Map - returns number of keys
 * List - returns number of elements.
 * A general empty() method, which works on Collections and Strings.
 * Support for the ternary operator 'a ? b : c' - and its GNU-C / "Elvis" variant 'a ?: c'.
 * Support for the Perl-like regex matching operators '=~' and '!~'
 * Support for the CSS3-inspired 'startsWith' and 'endsWith' operators '=^' and '=$'
 * Support for user-defined functions.
 * Misc : '+' has been overloaded to be use as a String concatenation operator
 *
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class Jexl3ConfigEnvironmentPreprocessor implements ConfigEnvironmentPreprocessor, EnvironmentAware {

    private JexlEngine jexlEngine;

    private JexlContext jc = new MapContext();
    private ConfigurableEnvironment environment;

    @PostConstruct
    public void init() {
        jexlEngine = new JexlBuilder().create();
        // 支持的参数
        // 1. 启动参数
        // 2. 测试配置变量
        // 3. 系统属性
        PropertySource<?> ps = environment.getPropertySources().get("systemProperties");
        Map<Object, Object> source = (Map<Object, Object>) ps.getSource();
        source.forEach((key, value) -> {
            if (!jc.has(keyed((String) key))) {
                jc.set(keyed((String) key), value);
            }
        });

        // 4. 环境变量
        ps = environment.getPropertySources().get("systemEnvironment");
        source = (Map<Object, Object>) ps.getSource();
        source.forEach((key, value) -> {
            if (!jc.has(keyed((String) key))) {
                jc.set(keyed((String) key), value);
            }
        });
    }

    private String keyed(String key) {
        String[] arr = key.split("[.]");

        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            if (sb.length() == 0) {
                sb.append(s);
            } else {
                sb.append(s.substring(0, 1).toUpperCase());
                sb.append(s.substring(1));
            }
        }

        return sb.toString();
    }


    @Override
    public String process(String key) {
        key = key.trim();
        if (key.startsWith("#{") && key.endsWith("}")) {
            int len = key.length();
            try {
                JexlExpression expr = jexlEngine.createExpression(key.substring(2, len - 1));
                return expr.evaluate(jc).toString();
            } catch (Exception e) {
                throw new PreprocessingException("error while evaluate expression: " + key, e);
            }
        }

        return key;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }
}
