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
package com.baidu.formula.ratelimiter.spring.boot.autoconfigure;

import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.config.entity.FormulaRateLimiterConfig;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.ratelimiter.RateLimiterManager;
import com.baidu.formula.ratelimiter.spring.boot.autoconfigure.util.FormulaConfigUtils;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by luoguangming on 2019/4/17.
 * Url global rateLimiter
 */
public class RateLimiterGlobalEffectiveFilter extends OncePerRequestFilter {

    private Logger logger = LoggerFactory.getLogger(RateLimiterGlobalEffectiveFilter.class);

    private RateLimiterManager rateLimiterManager;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    private static final String GLOBAL_URL_METHOD_STRING = "/global#*";

    private static final String DEFAULT_SKIP_PATTERN_STRING = "/api-docs.*|/swagger.*|"
            + ".*\\.png|.*\\.css|.*\\.js|.*\\.html|/favicon.ico|/hystrix.stream";

    public RateLimiterGlobalEffectiveFilter(RateLimiterManager rateLimiterManager) {
        this.rateLimiterManager = rateLimiterManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        handleFilterChain(request, response, filterChain);
    }

    private void handleFilterChain(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // get uri and convert to generic uri
        String uri = urlPathHelper.getRequestUri(request);
        logger.debug("Request Uri is {}", uri);
        if (isUriMatch(uri)) {
            try {
                FormulaRateLimiterConfig formulaRateLimiterConfig =
                        rateLimiterManager.getRatelimiterConfigs().get(GLOBAL_URL_METHOD_STRING);
                if (FormulaConfigUtils.isConfigSourceMatched(formulaRateLimiterConfig, request)) {
                    // waitForPermit
                    RateLimiter rateLimiter = rateLimiterManager.getRateLimiterFromRegistry(GLOBAL_URL_METHOD_STRING);
                    rateLimiterManager.waitForPermit(rateLimiter);
                    logger.debug("Succeed to get permission from global rate limiter!");
                }
            } catch (Exception e) {
                if (e instanceof ServletException || e instanceof IOException) {
                    throw e;
                }
                if (FormulaConfigUtils.isBlockException(response, e)) {
                    return;
                } else {
                    logger.error("RateLimiterGlobalEffectiveFilter: unexpected exception occurs in RateLimiter: ", e);
                }
            }
        }
        // doFilter
        filterChain.doFilter(request, response);
    }

    /**
     * Return true when skipPattern doesn't match
     * and RateLimiter Configuration set contains Global RateLimiter name.
     */
    private boolean isUriMatch(String uri) {
        Pattern defaultPattern = Pattern.compile(DEFAULT_SKIP_PATTERN_STRING);
        boolean shouldSkip = defaultPattern.matcher(uri).matches();
        boolean isGlobalRateLimiter = rateLimiterManager.getRatelimiterConfigs()
                .keySet().contains(GLOBAL_URL_METHOD_STRING);
        logger.debug("Should skip: {}, is Global RateLimiter configured: {}", shouldSkip, isGlobalRateLimiter);
        return !shouldSkip && isGlobalRateLimiter;
    }
}
