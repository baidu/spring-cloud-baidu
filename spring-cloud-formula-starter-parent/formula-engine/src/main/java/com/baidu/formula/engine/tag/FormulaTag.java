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
package com.baidu.formula.engine.tag;

import lombok.Data;

import java.util.List;

/**
 * Created by luoguangming on 2019/05/24.
 */
@Data
public class FormulaTag {

    private String key;

    private Operation op;

    private List<String> value;

    private TagType type;

    /**
     * 消除list中元素顺序对hashCode结果的影响
     * @return
     */
    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        int listHash = 0;
        for (String str : value) {
            listHash = listHash + (str != null ? str.hashCode() : 0);
        }
        result = 31 * result +  listHash;
        return result;
    }

}
