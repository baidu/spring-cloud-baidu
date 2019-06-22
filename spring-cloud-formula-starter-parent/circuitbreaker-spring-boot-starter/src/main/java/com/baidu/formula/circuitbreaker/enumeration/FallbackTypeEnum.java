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
package com.baidu.formula.circuitbreaker.enumeration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author modifid by cuiweizheng
 */
public enum FallbackTypeEnum {

    EXCEPTION(1, "抛出异常"),
    NULL(2, "返回null"),
    VALUE(3, "返回值"),
    FUNCTION(4, "调用方法");

    private static final Map<Integer, FallbackTypeEnum> map = new HashMap<Integer, FallbackTypeEnum>();

    static {
        for (FallbackTypeEnum mode : FallbackTypeEnum.values()) {
            map.put(mode.id, mode);
        }
    }

    private Integer id;
    private String name;

    private FallbackTypeEnum(Integer id, String name) {
        this.id = id;
        this.name = name;

    }

    public static FallbackTypeEnum getById(Number id) {
        if (id == null) {
            return null;
        }
        return map.get(id.intValue());
    }

    public static Map<Integer, FallbackTypeEnum> map() {
        return Collections.unmodifiableMap(map);
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}


