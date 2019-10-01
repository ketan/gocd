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

import com.thoughtworks.go.agent.http.RegistrationStatus;
import com.thoughtworks.go.agent.http.ServerApiClient;
import com.thoughtworks.go.agent.meta.AgentMeta;
import com.thoughtworks.go.agent.registration.AgentAutoRegistrationProperties;
import com.thoughtworks.go.agent.system.AgentNetworkInterface;
import com.thoughtworks.go.agent.system.GoAgentProperties;
import com.thoughtworks.go.agent.system.OperatingSystem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.thoughtworks.go.agent.http.RegistrationStatus.OK;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Slf4j
public class AgentInitializer {
    private final ServerApiClient client;
    private final TokenService tokenService;
    private final GuidService guidService;
    private final AgentNetworkInterface networkInterface;
    private final String currentWorkingDir = new File(".").getAbsolutePath();
    private GoAgentProperties goAgentProperties;
    @Getter
    private String token;
    @Getter
    private boolean registered;
    @Getter
    private String cookie;


    @Autowired
    public AgentInitializer(ServerApiClient client,
                            TokenService tokenService,
                            GuidService guidService,
                            GoAgentProperties goAgentProperties,
                            AgentNetworkInterface networkInterface) {
        this.client = client;
        this.tokenService = tokenService;
        this.guidService = guidService;
        this.goAgentProperties = goAgentProperties;
        this.networkInterface = networkInterface;
    }

    public void getTokenFromServerIfRequired() {
        if (isNotBlank(token)) {
            return;
        }

        if (tokenService.dataPresent()) {
            this.token = tokenService.load();
        } else {
            String token = client.getToken(guidService.load());
            if (isNotBlank(token)) {
                tokenService.store(token);
                this.token = token;
            }
        }
    }

    public void registerWithServerIfRequired() {
        if (isBlank(token)) {
            log.warn("Agent does not have token to register. Not performing registration.");
            return;
        }

        if (registered) {
            log.debug("Agent is already registered. Not performing registration.");
            return;
        }

        AgentAutoRegistrationProperties autoRegistrationProperties = new AgentAutoRegistrationProperties(goAgentProperties.autoRegisterProperties());
        RegistrationStatus register = client.register(agentMeta(), autoRegistrationProperties, token);
        if (register == OK) {
            this.registered = true;
        }
    }

    public void getCookieFromServerIfRequired() {
        if (isNotBlank(cookie)) {
            return;
        }

        cookie = client.getCookie(agentMeta(), token);
    }

    public AgentMeta agentMeta() {
        return new AgentMeta()
                .setUuid(guidService.load())
                .setHostname(networkInterface.getHostname())
                .setLocation(currentWorkingDir)
                .refreshUsableSpaceInPipelinesDir()
                .setIpAddress(networkInterface.getIpUsedToConnectWithServer())
                .setOperationSystem(OperatingSystem.getCompleteName());
    }
}
