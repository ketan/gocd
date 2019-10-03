/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.executors.WorkExecutor;
import com.thoughtworks.go.agent.http.ServerApiClient;
import com.thoughtworks.go.agent.services.AgentInitializer;
import com.thoughtworks.go.protobufs.work.WorkProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class LooperTest {
    @Mock
    private ServerApiClient client;
    @Mock
    private AgentInitializer agentInitializer;
    @Mock
    private WorkExecutor workExecutor;

    @InjectMocks
    private Looper looper;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void shouldCallAgentInitializerInOrder() {
        looper.loop();

        InOrder inOrder = inOrder(agentInitializer, client);

        inOrder.verify(agentInitializer).getTokenFromServerIfRequired();
        inOrder.verify(agentInitializer).registerWithServerIfRequired();
        inOrder.verify(agentInitializer).getCookieFromServerIfRequired();
        inOrder.verify(client).getWork(agentInitializer.getCookie(), agentInitializer.getToken(), agentInitializer.agentMeta());
    }

    @Test
    void shouldNotCallRegisterWithServerWhenGetTokenFromServerBombs() {
        doThrow(new RuntimeException("bomb!")).when(agentInitializer).getTokenFromServerIfRequired();

        assertThatCode(() -> looper.loop()).isInstanceOf(RuntimeException.class);

        verify(agentInitializer, never()).registerWithServerIfRequired();
        verify(agentInitializer, never()).getCookieFromServerIfRequired();
        verifyZeroInteractions(client);
        verifyZeroInteractions(workExecutor);
    }

    @Test
    void shouldNotCallGetCookieFromServerWhenRegisterWithServerBombs() {
        doThrow(new RuntimeException("bomb!")).when(agentInitializer).registerWithServerIfRequired();

        assertThatCode(() -> looper.loop()).isInstanceOf(RuntimeException.class);

        verify(agentInitializer, never()).getCookieFromServerIfRequired();
        verifyZeroInteractions(client);
        verifyZeroInteractions(workExecutor);
    }

    @Test
    void shouldNotGetWorkFromServerWhenGetCookieFromServerBombs() {
        doThrow(new RuntimeException("bomb!")).when(agentInitializer).getCookieFromServerIfRequired();

        assertThatCode(() -> looper.loop()).isInstanceOf(RuntimeException.class);

        verifyZeroInteractions(client);
        verifyZeroInteractions(workExecutor);
    }

    @Test
    void shouldExecuteWorkWhenServerReturnsAWork() {
        WorkProto work = WorkProto.newBuilder().build();
        Optional<WorkProto> optionalProtoWork = Optional.of(work);
        when(client.getWork(agentInitializer.getCookie(), agentInitializer.getToken(), agentInitializer.agentMeta()))
                .thenReturn(optionalProtoWork);

        looper.loop();

        verify(workExecutor).execute(work);
        verifyZeroInteractions(workExecutor);
    }

    @Test
    void shouldExecuteWorkWhenWorkThereIsNotWork() {
        Optional<WorkProto> optionalProtoWork = Optional.empty();
        when(client.getWork(agentInitializer.getCookie(), agentInitializer.getToken(), agentInitializer.agentMeta()))
                .thenReturn(optionalProtoWork);

        looper.loop();

        verifyZeroInteractions(workExecutor);
    }
}