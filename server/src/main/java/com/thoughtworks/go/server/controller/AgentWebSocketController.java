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

package com.thoughtworks.go.server.controller;


import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.messaging.scheduling.WorkAssignments;
import com.thoughtworks.go.server.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class AgentWebSocketController {
    private final WorkAssignments workAssignments;
    private final AgentService agentService;

    @Autowired
    public AgentWebSocketController(WorkAssignments workAssignments,
                                    AgentService agentService) {
        this.workAssignments = workAssignments;
        this.agentService = agentService;
    }

    @SendToUser("/topic/work")
    private void sendWork() {
        AgentInstances agentInstances = agentService.getAgentInstances();

        agentInstances.forEach(agentInstance -> {
            if (agentInstance.isRegistered() && agentInstance.isIdle()) {

            }
        });
    }

}
