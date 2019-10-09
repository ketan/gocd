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

import com.google.protobuf.MessageLite;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AgentMessageHandler implements StompSessionHandler {
    private final AgentWebSocketListener agentWebSocketListener;
    private Map<String, Meta> cacheListenerMethods;

    @Autowired
    public AgentMessageHandler(AgentWebSocketListener agentWebSocketListener) {
        this.agentWebSocketListener = agentWebSocketListener;
        this.cacheListenerMethods = buildListenerMethods();
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Agent is connected to {}.", connectedHeaders.getDestination());
        cacheListenerMethods.keySet().forEach(topic -> subscribeTo(topic, session));
    }

    @Override
    public void handleException(StompSession session,
                                StompCommand command,
                                StompHeaders headers,
                                byte[] payload,
                                Throwable exception) {
        log.error("Failed to parse payload from server.", exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("Connection failure.", exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        log.info("Payload type {}", headers.getDestination());
        return getListenerFor(headers.getDestination()).responseType;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        log.info("Object type {}", headers.getDestination());
        try {
            getListenerFor(headers.getDestination()).method.invoke(agentWebSocketListener, payload);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void subscribeTo(String destination, StompSession session) {
        log.info("Subscribing to topic {}.", destination);
        final StompSession.Subscription subscription = session.subscribe(destination, this);
        log.info("Subscription to {} is completed. Subscription id is {}.", destination, subscription.getSubscriptionId());
    }

    static class Meta {
        private final Class<? extends MessageLite> responseType;
        private final Method method;

        Meta(Class<? extends MessageLite> responseType, Method method) {
            this.responseType = responseType;
            this.method = method;
        }
    }

    private Map<String, Meta> buildListenerMethods() {
        final Map<String, Meta> _cache = new HashMap<>();
        final Method[] allMethods = agentWebSocketListener.getClass().getDeclaredMethods();
        for (Method method : allMethods) {
            if (method.isAnnotationPresent(ListenTo.class)) {
                final ListenTo listenTo = method.getAnnotation(ListenTo.class);
                final Meta meta = new Meta(listenTo.responseType(), method);
                _cache.put(listenTo.target(), meta);
            }
        }
        return _cache;
    }

    private Meta getListenerFor(String destination) {
        final Meta meta = cacheListenerMethods.get(destination);
        if (meta == null) {
            throw new RuntimeException("No listener defined for " + destination);
        }
        return meta;
    }
}
