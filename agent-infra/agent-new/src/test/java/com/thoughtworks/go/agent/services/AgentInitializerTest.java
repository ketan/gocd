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

package com.thoughtworks.go.agent.services;

import com.thoughtworks.go.agent.http.ServerApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class AgentInitializerTest {
    @Mock
    private ServerApiClient client;
    @Mock
    private TokenService tokenService;
    @Mock
    private GuidService guidService;

    @InjectMocks
    private AgentInitializer agentInitializer;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Nested
    class GetTokenFromServerIfRequired {
        @Test
        void shouldReturnTokenFromDiskWhenPresent() {
            when(tokenService.dataPresent()).thenReturn(true);

            agentInitializer.getTokenFromServerIfRequired();

            verify(tokenService).load();
        }

        @Test
        void shouldGetTokenFromServerWhenOneIsNotPresentOnDisk() {
            String guid = "foo-guid";
            when(guidService.load()).thenReturn(guid);
            when(tokenService.dataPresent()).thenReturn(false);

            agentInitializer.getTokenFromServerIfRequired();

            verify(client).getToken(guid);
            verify(tokenService, never()).load();
        }
    }
}