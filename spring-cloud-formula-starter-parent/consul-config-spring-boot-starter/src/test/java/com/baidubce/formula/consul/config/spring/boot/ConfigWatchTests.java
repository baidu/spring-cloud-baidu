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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.baidubce.formula.consul.config.spring.boot.auth.BmsAuthClient;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static com.baidubce.formula.consul.config.spring.boot.Format.FILES;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author luoguangming
 */
@ActiveProfiles("logging-test")
public class ConfigWatchTests {

    private ConsulConfigProperties configProperties;

    private BmsAuthClient bmsAuthClient;

    @Before
    public void setUp() throws Exception {
        this.configProperties = new ConsulConfigProperties();
        this.bmsAuthClient = new BmsAuthClient();

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.TRACE);
    }

    @Test
    public void watchPublishesEvent() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        setupWatch(eventPublisher, "/app/");

        verify(eventPublisher, times(1)).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchWithNullValueDoesNotPublishEvent() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Response<List<GetValue>> response = new Response<>(null, 1l, false, 1L);

        setupWatch(eventPublisher, "/app/", response);

        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchForFileFormatPublishesEvent() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        this.configProperties.setFormat(FILES);

        setupWatch(eventPublisher, "/config/app.yml");

        verify(eventPublisher, times(1)).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchWithNullIndexDoesNotPublishEvent() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Response<List<GetValue>> response = new Response<>(Arrays.asList(new GetValue()), null, false, 1L);

        setupWatch(eventPublisher, "/app/", response);

        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchWithIllegalIndexDoesNotPublishEvent() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Response<List<GetValue>> response = new Response<>(Arrays.asList(new GetValue()), 0l, false, 1L);

        setupWatchWithIllegalIndex(eventPublisher, "/app/", response);

        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchWithNullResponse() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Response<List<GetValue>> response = null;

        setupWatch(eventPublisher, "/app/", response);

        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchWithOperationException() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        setupWatchThrowException(eventPublisher, "/app/");

        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchWithExceptionAndFailFast() {
        this.configProperties.setFailFast(true);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        setupWatch(eventPublisher, "/app/", null);

        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void watchWithExceptionAndWarnLog() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        setupWatch(eventPublisher, "/app/", null);

        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    private void setupWatch(ApplicationEventPublisher eventPublisher, String context) {
        Response<List<GetValue>> response = new Response<>(Arrays.asList(new GetValue()), 1L, false, 1L);
        this.setupWatch(eventPublisher, context, response);
    }

    private void setupWatch(ApplicationEventPublisher eventPublisher, String context,
                            Response<List<GetValue>> response) {
        ConsulClient consul = mock(ConsulClient.class);

        when(consul.getKVValues(ArgumentMatchers.eq(context), nullable(String.class),
                ArgumentMatchers.any(QueryParams.class))).thenReturn(response);

        LinkedHashMap<String, Long> initialIndexes = new LinkedHashMap<>();
        initialIndexes.put(context, 0L);
        startWatch(eventPublisher, consul, initialIndexes);
    }

    private void setupWatchThrowException(ApplicationEventPublisher eventPublisher, String context) {
        ConsulClient consul = mock(ConsulClient.class);
        OperationException operationException = new OperationException(403, null, null);
        when(consul.getKVValues(ArgumentMatchers.eq(context), nullable(String.class),
                ArgumentMatchers.any(QueryParams.class))).thenThrow(operationException);

        LinkedHashMap<String, Long> initialIndexes = new LinkedHashMap<>();
        initialIndexes.put(context, 0L);
        startWatch(eventPublisher, consul, initialIndexes);
    }

    private void setupWatchWithIllegalIndex(ApplicationEventPublisher eventPublisher, String context,
                                            Response<List<GetValue>> response) {
        ConsulClient consul = mock(ConsulClient.class);

        when(consul.getKVValues(ArgumentMatchers.eq(context), nullable(String.class),
                ArgumentMatchers.any(QueryParams.class))).thenReturn(response);

        LinkedHashMap<String, Long> initialIndexes = new LinkedHashMap<>();
        initialIndexes.put(context, -1L);
        startWatch(eventPublisher, consul, initialIndexes);
    }

    private void startWatch(ApplicationEventPublisher eventPublisher,
                            ConsulClient consul, LinkedHashMap<String, Long> initialIndexes) {
        ConfigWatch watch = new ConfigWatch(this.configProperties, consul, bmsAuthClient, initialIndexes);
        watch.setApplicationEventPublisher(eventPublisher);

        watch.start();

        try {
            // wait until watch is triggered asynchronously
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetMethods() {
        LinkedHashMap<String, Long> initialIndexes = new LinkedHashMap<>();
        initialIndexes.put("/context", 0L);
        ConfigWatch watch = new ConfigWatch(this.configProperties, new ConsulClient(), bmsAuthClient, initialIndexes);
        Assert.assertEquals(0, watch.getPhase());
        Assert.assertEquals(true, watch.isAutoStartup());
    }

    @Test
    public void testConstructor() {
        LinkedHashMap<String, Long> initialIndexes = new LinkedHashMap<>();
        initialIndexes.put("/context/", -1L);
        List<ThreadPoolTaskScheduler> threadPoolTaskSchedulers = new ArrayList<>();
        ConfigWatch watch = new ConfigWatch(this.configProperties, null, bmsAuthClient, initialIndexes,
                threadPoolTaskSchedulers);
        Assert.assertNotNull(watch);
    }

    @Test
    public void testStop() {
        LinkedHashMap<String, Long> initialIndexes = new LinkedHashMap<>();
        initialIndexes.put("/context", 0L);
        ConfigWatch watch = new ConfigWatch(this.configProperties, new ConsulClient(), bmsAuthClient, initialIndexes);
        watch.start();
        Assert.assertTrue(watch.isRunning());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // do nothing
            }
        };
        watch.stop(runnable);
        Assert.assertFalse(watch.isRunning());
    }

    @Test
    public void testRefreshEventData() {
        String context = "/test";
        Long preIndex = 1l;
        Long newIndex = 2l;
        ConfigWatch.RefreshEventData refreshEventData = new ConfigWatch.RefreshEventData(context, preIndex, newIndex);
        Assert.assertEquals(context, refreshEventData.getContext());
        Assert.assertEquals(preIndex, refreshEventData.getPrevIndex());
        Assert.assertEquals(newIndex, refreshEventData.getNewIndex());
        Assert.assertTrue(refreshEventData.equals(refreshEventData));
        Assert.assertFalse(refreshEventData == null);
        Assert.assertNotNull(refreshEventData.hashCode());
        ConfigWatch.RefreshEventData refreshEventData2 = new ConfigWatch.RefreshEventData(context, preIndex, newIndex);
        Assert.assertTrue(refreshEventData.equals(refreshEventData2));
    }

    @Test
    public void firstCallDoesNotPublishEvent() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        this.configProperties.setFormat(FILES);

        GetValue getValue = new GetValue();
        String context = "/config/app.yml";
        ConsulClient consul = mock(ConsulClient.class);
        List<GetValue> getValues = Collections.singletonList(getValue);

        Response<List<GetValue>> response = new Response<>(getValues, 1L, false, 1L);
        when(consul.getKVValues(ArgumentMatchers.eq(context), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(QueryParams.class)))
                .thenReturn(response);

        ConfigWatch watch = new ConfigWatch(this.configProperties, consul, bmsAuthClient,
                new LinkedHashMap<String, Long>());
        watch.setApplicationEventPublisher(eventPublisher);

        watch.watchConfigKeyValues(context);
        verify(eventPublisher, times(0)).publishEvent(ArgumentMatchers.any(RefreshEvent.class));
    }

    @Test
    public void testBmsHttpTransportConstructor() {
        BmsHttpTransport bmsHttpTransport = new BmsHttpTransport();
        Assert.assertNotNull(bmsHttpTransport);
        Assert.assertNotNull(bmsHttpTransport.getHttpClient());
    }
}
