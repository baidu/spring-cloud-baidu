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
package com.baidu.formula.test.timelimiter;

import com.baidu.formula.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
@SpringBootApplication
@EnableAspectJAutoProxy
public class TimeLimiterApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeLimiterApplication.class, args);
    }
}

@Controller
class LogicController {
    @RequestMapping("/timing")
    @ResponseBody
    @CircuitBreaker
    public Object logic(@RequestParam(value = "duration", defaultValue = "0") double duration)
            throws InterruptedException {
        Thread.sleep((long) (duration * 1000));

        return "{\"duration\": " + duration + ", \"status\" : \"ok\"}";
    }

    @RequestMapping("/free")
    @ResponseBody
    public Object free(@RequestParam(value = "duration", defaultValue = "0") double duration)
            throws InterruptedException {
        Thread.sleep((long) (duration * 1000));
        return "{\"duration\": " + duration + ", \"status\" : \"ok\"}";
    }

    @RequestMapping("circuit")
    @ResponseBody
    @CircuitBreaker(fallback = "circuitFallback")
    public Object circuit(@RequestParam(value = "duration", defaultValue = "0") double duration,
                          @RequestParam(value = "success-rate", defaultValue = "0") int successRate)
            throws InterruptedException {
        Thread.sleep((long) (duration * 1000));
        Random random = new Random();
        int val = random.nextInt(100);
        if (val >= successRate ) {
            throw new RuntimeException("error, with random value: " + val);
        }

        return "{\"duration\": " + duration + ", \"status\" : \"ok\"}";
    }

    public Object circuitFallback(@RequestParam(value = "duration", defaultValue = "0") double duration,
                                  @RequestParam(value = "success-rate", defaultValue = "0") int successRate) {

        return "{\"duration\": " + duration + ", \"status\" : \"circuit opened\"}";
    }

    public Object circuitFallback(@RequestParam(value = "duration", defaultValue = "0") double duration,
                                  @RequestParam(value = "success-rate", defaultValue = "0") int successRate,
                                  Throwable throwable) {

        return "{\"duration\": " + duration + ", \"status\" : \"circuit opened\"}";
    }
}

@Controller
class LogicController2 {
    @RequestMapping("/free2")
    @ResponseBody
    public Object free(@RequestParam(value = "duration", defaultValue = "0") double duration)
            throws InterruptedException {
        Thread.sleep((long) (duration * 1000));
        return "{\"duration\": " + duration + ", \"status\" : \"ok\"}";
    }
}