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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by liuruisen on 2019/1/8.
 * url rateLimiter
 */
public class RateLimiterEffectiveFilter extends OncePerRequestFilter {

    private Logger logger = LoggerFactory.getLogger(RateLimiterEffectiveFilter.class);

    private RateLimiterManager rateLimiterManager;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();


    public RateLimiterEffectiveFilter(RateLimiterManager rateLimiterManager) {
        this.rateLimiterManager = rateLimiterManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        handleFilterChain(request, response, filterChain);
    }

    private void handleFilterChain(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // get uri and convert to generic uri
            String uri = urlPathHelper.getRequestUri(request);
            logger.debug("Request Uri is {}", uri);
            if (rateLimiterManager.getPatternsRequestMap().size() > 0) {
                String limiterName = null;
                // search for generic uri
                for (PatternsRequestCondition patternsRequest : rateLimiterManager.getPatternsRequestMap().values()) {
                    List<String> matchers = patternsRequest.getMatchingPatterns(uri);
                    if (matchers != null && matchers.size() > 0) {
                        // http limiter name is 'uri' + '#' + 'httpmethod'
                        limiterName = matchers.get(0) + "#" + request.getMethod().toLowerCase();
                        logger.debug("Uri RateLimiter Name: {}", limiterName);
                        break;
                    }
                }
                if (!StringUtils.isEmpty(limiterName)) {
                    FormulaRateLimiterConfig formulaRateLimiterConfig =
                            rateLimiterManager.getRatelimiterConfigs().get(limiterName);
                    if (FormulaConfigUtils.isConfigSourceMatched(formulaRateLimiterConfig, request)) {
                        // waitForPermit
                        RateLimiter rateLimiter = rateLimiterManager.getRateLimiterFromRegistry(limiterName);
                        rateLimiterManager.waitForPermit(rateLimiter);
                        logger.debug("Succeed to get permission from rate limiter!");
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof ServletException || e instanceof IOException) {
                throw e;
            }
            if (FormulaConfigUtils.isBlockException(response, e)) {
                return;
            } else {
                logger.error("RateLimiterEffectiveFilter: unexpected exception occurs in RateLimiter: ", e);
            }
        }
        // doFilter
        filterChain.doFilter(request, response);
    }
}