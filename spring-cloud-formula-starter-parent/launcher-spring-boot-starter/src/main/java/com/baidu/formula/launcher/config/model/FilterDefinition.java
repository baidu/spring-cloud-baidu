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
package com.baidu.formula.launcher.config.model;

import java.util.List;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class FilterDefinition {
    public static final int ORDER_HIGHEST = Integer.MIN_VALUE;

    public static final int ORDER_LOWEST = Integer.MAX_VALUE;

    private String id;
    private int order = 0;

    private List<PredicateDefinition> predicates;
    private List<ActionDefinition> actions;

}
