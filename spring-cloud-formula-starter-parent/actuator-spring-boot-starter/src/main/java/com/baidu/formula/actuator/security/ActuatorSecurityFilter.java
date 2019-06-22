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
package com.baidu.formula.actuator.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Bowu Dong (tq02ksu@gmail.com)
 */
public class ActuatorSecurityFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ActuatorSecurityFilter.class);

    private Set<String> permittedAddrs;

    @Override
    public void init(FilterConfig filterConfig) {
        permittedAddrs = new HashSet<>(Arrays.asList("127.0.0.1", "localhost", "0:0:0:0:0:0:0:1", "::1"));

        try {
            InetAddress addr = InetAddress.getLocalHost();
            permittedAddrs.add(addr.getHostAddress());
            permittedAddrs.add(addr.getHostName());
        } catch (UnknownHostException e) {
            logger.warn("can't determine localhost address or ip address");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        if (uri.equals("/actuator/health")) {
            doFilterForHealth(req, (HttpServletResponse) response, chain);
            return;
        }

        String ip = req.getRemoteAddr();
        if (permittedAddrs.contains(ip)) {
            chain.doFilter(req, response);
        }
    }

    private void doFilterForHealth(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);

        chain.doFilter(request, wrapper);
        String ip = request.getRemoteAddr();
        if (permittedAddrs.contains(ip)) {
            wrapper.copyBodyToResponse();
            return;
        }

        byte[] bytes = wrapper.getContentAsByteArray();
        int status = wrapper.getStatus();
        Map<String, Collection<String>> headers = wrapper.getHeaderNames()
                .stream().collect(Collectors.toMap(Function.identity(), wrapper::getHeaders));
        logger.info("overwrite response content, original={}", new String(bytes, "utf-8"));
        String result = wrapper.getStatus() == 200 ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}";
        wrapper.reset();
        wrapper.setStatus(status);
        headers.forEach( (key, val) ->  val.forEach(v -> wrapper.addHeader(key, v)));
        try (OutputStream out = wrapper.getOutputStream()) {
            out.write(result.getBytes("UTF-8"));
        }

        wrapper.copyBodyToResponse();
    }

    @Override
    public void destroy() {

    }
}
