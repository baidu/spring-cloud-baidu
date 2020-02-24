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

import com.baidubce.formula.consul.config.spring.boot.auth.BmsAuthClient;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;

/**
 * @author luoguangming
 */
public class ConsulFilesPropertySource extends ConsulPropertySource {

    public ConsulFilesPropertySource(String context, ConsulClient source, BmsAuthClient bmsAuthClient,
                                     ConsulConfigProperties configProperties) {
        super(context, source, bmsAuthClient, configProperties);
    }

    @Override
    public void init() {
    }

    public void init(GetValue getValue) {
        if (this.getContext().endsWith(".yml") || this.getContext().endsWith(".yaml")) {
            parseValue(getValue, Format.YAML);
        } else if (this.getContext().endsWith(".properties")) {
            parseValue(getValue, Format.PROPERTIES);
        } else {
            throw new IllegalStateException(
                    "Unknown files extension for context " + this.getContext());
        }
    }

}
