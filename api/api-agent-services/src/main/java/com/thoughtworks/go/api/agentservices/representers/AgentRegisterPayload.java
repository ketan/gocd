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

package com.thoughtworks.go.api.agentservices.representers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.domain.IpAddress;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@ToString
@Slf4j
public class AgentRegisterPayload {
    @JsonProperty("hostname")
    private String hostname;

    private String ipAddress;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("location")
    private String location;
    @JsonProperty("usable_space")
    private Long usableSpace;
    @JsonProperty("operating_system")
    private String operatingSystem;

    @JsonProperty("auto_register")
    private AutoRegisterPayload autoRegister;

    @Getter
    @Setter
    @Accessors(chain = true)
    @NoArgsConstructor
    @ToString
    public static class AutoRegisterPayload {
        @JsonProperty("environments")
        private List<String> environments;
        @JsonProperty("resources")
        private List<String> resources;
        @JsonProperty("hostname")
        private String hostname;
        @JsonProperty("elastic_agent_id")
        private String elasticAgentId;
        @JsonProperty("elastic_plugin_id")
        private String elasticPluginId;

        public boolean isElastic() {
            return isNoneBlank(elasticAgentId, elasticPluginId);
        }

        boolean partialElasticAgentInformation() {
            return !isElastic() && (isNotBlank(elasticAgentId) || isNotBlank(elasticPluginId));
        }

        void validate() {
            if (isElastic() && CollectionUtils.isNotEmpty(resources)) {
                throw new BadRequestException("Elastic agents cannot have resources.");
            }

            if (partialElasticAgentInformation()) {
                throw new BadRequestException("Elastic agents must submit both elastic_agent_id and elastic_plugin_id.");
            }
        }
    }

    public void bombIfInvalid() {
        if (ipAddress != null && isBlank(ipAddress)) {
            throw new BadRequestException("IpAddress cannot be empty if it is present.");
        }

        try {
            IpAddress.create(ipAddress);
        } catch (Exception e) {
            throw new BadRequestException(format("'%s' is an invalid IP address.", ipAddress));
        }

        if (isBlank(uuid)) {
            throw new BadRequestException("UUID cannot be empty.");
        }

        if (autoRegister != null) {
            autoRegister.validate();
        }
    }

    public Agent toAgent() {
        bombIfInvalid();
        Agent agent = new Agent(uuid, hostname, ipAddress);
        agent.setEnvironmentsFrom(autoRegister.environments);
        if (autoRegister.isElastic()) {
            agent.setElasticAgentId(autoRegister.elasticAgentId);
            agent.setElasticPluginId(autoRegister.elasticPluginId);
        } else {
            agent.setResourcesFromList(autoRegister.resources);
        }
        return agent;
    }

    public AgentRuntimeInfo toRuntimeInfo(boolean registeredAlready) {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(toAgent(), registeredAlready, location, usableSpace, operatingSystem);
        if (autoRegister != null && autoRegister.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, autoRegister.elasticAgentId, autoRegister.elasticPluginId);
        }
        return agentRuntimeInfo;
    }
}
