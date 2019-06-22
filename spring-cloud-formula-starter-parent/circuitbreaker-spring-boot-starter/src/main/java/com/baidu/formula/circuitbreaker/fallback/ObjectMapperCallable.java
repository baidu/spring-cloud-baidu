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
package com.baidu.formula.circuitbreaker.fallback;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class ObjectMapperCallable implements Callable<Object> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String result;

    private final Method method;

    public ObjectMapperCallable(String result, Method method) {
        this.result = result;
        this.method = method;
    }

    @Override
    public Object call() throws Exception {
        Type type = method.getGenericReturnType();

        JavaType t = objectMapper.constructType(type);
        return objectMapper.readValue(result, t);
    }

    @Override
    public String toString() {
        return super.toString() + "{" + "result='" + result + '\'' + ", type=" + method.getGenericReturnType() + '}';
    }
}
