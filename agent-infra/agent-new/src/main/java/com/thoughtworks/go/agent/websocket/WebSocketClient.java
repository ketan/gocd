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

package com.thoughtworks.go.agent.websocket;

import com.thoughtworks.go.agent.cli.AgentBootstrapperArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Component
public class WebSocketClient {
    private final WebSocketStompClient stompClient;
    private final AgentMessageHandler agentMessageHandler;
    private StompSession session;
    private AgentBootstrapperArgs bootstrapperArgs;
    private static final String WEBSOCKET_PATH = "agent-websocket";

    @Autowired
    public WebSocketClient(AgentMessageHandler agentMessageHandler,
                           AgentBootstrapperArgs bootstrapperArgs) {
        this.agentMessageHandler = agentMessageHandler;
        this.bootstrapperArgs = bootstrapperArgs;
        stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(
                        new WebSocketTransport(new StandardWebSocketClient())
                ))
        );
    }

    public void tryEstablishingConnection() {
        try {
            if (session != null && session.isConnected()) {
                log.debug("Agent is already connected with server {}.", bootstrapperArgs.getServerUrl());
                return;
            }

            log.debug("Connecting to server using url {}.", bootstrapperArgs.getServerUrl());
            session = stompClient.connect(websocketUrl(), agentMessageHandler)
                    .get(1, SECONDS);
            log.debug("Connected.");

        } catch (Exception e) {
            log.error("Some error: ", e);
            throw new RuntimeException(e);
        }
    }

    private String websocketUrl() {
        return String.format("ws://%s:%d/go/" + WEBSOCKET_PATH, bootstrapperArgs.getServerUrl().getHost(), 8153);
    }
}
