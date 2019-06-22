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
package com.baidu.formula.launcher.exception;

/**
 * Created by baidu on 09/02/2018.
 *
 * @author Bowu Dong (tq02ksu@gmail.com)
 * @date 09/02/2018
 */
public class LaunchingException extends RuntimeException {
    public LaunchingException() {
    }

    public LaunchingException(String message) {
        super(message);
    }

    public LaunchingException(String message, Throwable cause) {
        super(message, cause);
    }

    public LaunchingException(Throwable cause) {
        super(cause);
    }

    public LaunchingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
