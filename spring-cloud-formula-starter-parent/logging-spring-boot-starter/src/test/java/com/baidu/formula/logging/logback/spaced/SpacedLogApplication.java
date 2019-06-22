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
package com.baidu.formula.logging.logback.spaced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@SpringBootApplication
public class SpacedLogApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpacedLogApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpacedLogApplication.class, args);
    }

    @Bean
    public CommandLineRunner clr() {
        return args -> {
            logger.info("command line runner started!");
            LoggerFactory.getLogger("root").info("console here");
        };
    }
}
