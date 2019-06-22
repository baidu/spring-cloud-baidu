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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure.util;

import com.baidu.formula.engine.tag.FormulaTag;
import com.baidu.formula.engine.tag.Operation;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.entity.FormulaRateLimiterConfig;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.exception.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luoguangming on 2019/05/27.
 */
public class FormulaConfigUtils {

    private static Logger logger = LoggerFactory.getLogger(FormulaConfigUtils.class);

    public static boolean isConfigSourceMatched(FormulaRateLimiterConfig formulaRateLimiterConfig,
                                                HttpServletRequest request) {
        if (formulaRateLimiterConfig.getSource() == null) {
            // Vacancy in config.source means matching for all source
            return true;
        }
        List<FormulaTag> formulaTags = formulaRateLimiterConfig.getSource().getTags();
        if (formulaTags == null || formulaTags.size() == 0) {
            // Vacancy in config.source.tags means matching for all source
            return true;
        }
        for (FormulaTag sourceTag : formulaTags) {
            // when there is sourceTag mismatch, break and return false
            if (!isSourceTagMatched(sourceTag, request)) {
                return false;
            }
        }
        // only when all tags match will it return true.
        return true;
    }

    public static boolean isSourceTagMatched(FormulaTag sourceTag, HttpServletRequest request) {
        Operation operation = sourceTag.getOp();
        String key = sourceTag.getKey();
        List<String> values = sourceTag.getValue();
        String targetValue = request.getHeader(key);
        if (values == null || values.size() == 0) {
            logger.error("Values are empty in SourceTag !");
            return false;
        }
        if (targetValue == null || targetValue == "") {
            logger.error("Value is empty in HttpRequest header for key: {}", key);
            return false;
        }
        Boolean match = Operation.isOperationMatch(operation, values, targetValue);
        logger.debug("Match: {}, for sourceTag: {}, targetValue: {}", match, sourceTag.toString(), targetValue);
        return match;
    }

    public static boolean isRateLimiterRuleMatch(FormulaRateLimiterConfig formulaRateLimiterConfig) {
        if (formulaRateLimiterConfig.getDestination() == null) {
            // Vacancy in config.destination means matching by default
            return true;
        }
        List<FormulaTag> formulaTags = formulaRateLimiterConfig.getDestination().getTags();
        if (formulaTags == null || formulaTags.size() == 0) {
            // Vacancy in config.destination.tags means matching by default
            return true;
        }
        for (FormulaTag destinationTag : formulaTags) {
            // when there is destinationTag mismatch, break and return false
            if (!isDestinationTagMatched(destinationTag)) {
                return false;
            }
        }
        // only when all tags match will it return true.
        return true;
    }

    public static boolean isDestinationTagMatched(FormulaTag tag) {
        Operation operation = tag.getOp();
        String key = tag.getKey();
        List<String> values = tag.getValue();
        String targetValue = System.getenv(key);
        if (values == null || values.size() == 0) {
            logger.error("Values are empty in DestinationTag !");
            return false;
        }
        if (targetValue == null || targetValue == "") {
            logger.error("Value is empty in env for key: {}", key);
            return false;
        }
        Boolean match = Operation.isOperationMatch(operation, values, targetValue);
        logger.debug("Match: {}, for DestinationTag: {}, targetValue: {}", match, tag.toString(), targetValue);
        return match;
    }

    public static boolean isBlockException(HttpServletResponse response, Exception e) throws IOException {
        if (e instanceof BlockException) {
            response.setStatus(429); // too many request
            response.setContentType("application/json; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            response.flushBuffer();
            return true;
        } else {
            return false;
        }
    }

    public static Map<String, String> getSystemTags(){
        Map systemTags = new HashMap();
        systemTags.put(SystemTag.EM_APP, System.getenv(SystemTag.EM_APP));
        logger.debug("Insert {} into httpHeader: {}", SystemTag.EM_APP, System.getenv(SystemTag.EM_APP));
        systemTags.put(SystemTag.EM_PLATFORM, System.getenv(SystemTag.EM_PLATFORM));
        logger.debug("Insert {} into httpHeader: {}",SystemTag.EM_PLATFORM, System.getenv(SystemTag.EM_PLATFORM));
        return systemTags;
    }
}
