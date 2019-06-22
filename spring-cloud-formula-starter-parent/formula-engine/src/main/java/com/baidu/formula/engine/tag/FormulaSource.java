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
 * Created by luoguangming on 2019/05/27.
 * Embedded in a class for future extension, e.g. httpHeaders.
 */
@Data
public class FormulaSource {

    private List<FormulaTag> tags;

    @Override
    public int hashCode() {
        int result = 0;
        for (FormulaTag tag : tags) {
            result = result + (tag != null ? tag.hashCode() : 0);
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        FormulaSource tag = (FormulaSource)object;
        if (this == tag) {
            return true;
        }
        if (tag == null || getClass() != tag.getClass()) {
            return false;
        }
        if (tag.hashCode() == this.hashCode()) {
            return true;
        }
        return false;
    }

}
