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

import com.thoughtworks.go.agent.http.ServerApiClient;
import com.thoughtworks.go.agent.services.AgentInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Looper {
    private final ServerApiClient client;
    private final AgentInitializer agentInitializer;

    @Autowired
    public Looper(ServerApiClient client, AgentInitializer agentInitializer) {
        this.client = client;
        this.agentInitializer = agentInitializer;
    }

    @Scheduled(fixedDelayString = "${go.agent.get.work.interval}")
    public void loop() {
        log.debug("[Agent Loop] Trying to retrieve work.");
        agentInitializer.createTokenIfRequired();
        agentInitializer.registerIfRequired();
        agentInitializer.getCookiesIfRequired();
    }
}