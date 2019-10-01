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

package com.thoughtworks.go.agent.http;

import com.thoughtworks.go.agent.cli.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.meta.AgentMeta;
import com.thoughtworks.go.agent.registration.AgentAutoRegistrationProperties;
import com.thoughtworks.go.protobufs.registration.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.*;

@Slf4j
@Component
public class GoCDServerAPIProxy {
    private final CloseableHttpClient httpClient;
    private final GoServerApiUrls apiUrlHelper;

    @Autowired
    public GoCDServerAPIProxy(CloseableHttpClient httpClient, AgentBootstrapperArgs agentBootstrapperArgs) {
        this.httpClient = httpClient;
        this.apiUrlHelper = new GoServerApiUrls(agentBootstrapperArgs.getServerUrl());
    }

    public String getToken(String uuid) {
        HttpPost request = new HttpPost(apiUrlHelper.tokenUrl());
        setContentType(request);
        request.setEntity(new ByteArrayEntity(UUID.newBuilder().setUuid(uuid).build().toByteArray()));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != SC_OK) {
                throw logResponseAndThrowError(response);
            }

            log.info("The server has generated token for the agent.");
            return Token.parseFrom(response.getEntity().getContent()).getToken();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private RuntimeException logResponseAndThrowError(CloseableHttpResponse response) throws IOException {
        final String responseBody = responseBody(response);
        log.error("The server responded with:");
        log.error(Arrays.stream(response.getAllHeaders()).map(Object::toString).collect(Collectors.joining("\n")));
        log.error(response.getStatusLine().toString());
        log.error(responseBody);
        throw new RuntimeException(responseBody);
    }

    public RegistrationStatus register(AgentMeta agentMeta,
                                       AgentAutoRegistrationProperties autoRegistrationProperties,
                                       String token) {

        RegistrationPayload registrationPayload = RegistrationPayload.newBuilder()
                .setAgentMeta(toProtobuf(agentMeta))
                .setAutoRegister(toProtobuf(autoRegistrationProperties))
                .build();

        HttpPost post = new HttpPost(apiUrlHelper.registerUrl());
        setContentType(post);
        addAuthenticationHeaders(post, agentMeta, token);
        post.setHeader("X-Auto-Register-Key", autoRegistrationProperties.agentAutoRegisterKey());
        post.setEntity(new ByteArrayEntity(registrationPayload.toByteArray()));

        log.info("[Agent Registration] Starting to register agent.");
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            switch (statusCode) {
                case SC_OK:
                    log.info("[Agent Registration] Successfully registered agent.");
                    return RegistrationStatus.OK;
                case SC_ACCEPTED:
                    log.debug("The server has accepted the registration request.");
                    return RegistrationStatus.PENDING;
                case SC_UNAUTHORIZED:
                    log.warn("Server denied registration request due to invalid token. Deleting existing token from disk.");
                    return RegistrationStatus.UNAUTHORIZED;
                default:
                    throw logResponseAndThrowError(response);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getCookie(AgentMeta agentMeta, String token) {
        com.thoughtworks.go.protobufs.registration.AgentMeta protobuf = toProtobuf(agentMeta);
        HttpPost post = new HttpPost(apiUrlHelper.cookieUrl());
        setContentType(post);
        addAuthenticationHeaders(post, agentMeta, token);
        post.setEntity(new ByteArrayEntity(protobuf.toByteArray()));

        log.info("About to get cookie from the server.");
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() == SC_OK) {
                Cookie cookie = Cookie.parseFrom(response.getEntity().getContent());
                log.info("Got cookie: {}", cookie.getCookie());
                return cookie.getCookie();
            }
            throw logResponseAndThrowError(response);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private void addAuthenticationHeaders(HttpPost post, AgentMeta agentMeta, String token) {
        post.setHeader("X-Agent-GUID", agentMeta.getUuid());
        post.setHeader("Authorization", token);
    }

    private AgentAutoRegistration toProtobuf(AgentAutoRegistrationProperties autoRegistrationProperties) {
        return AgentAutoRegistration.newBuilder()
                .setHostname(autoRegistrationProperties.agentAutoRegisterHostname())
                .addAllEnvironments(autoRegistrationProperties.agentAutoRegisterEnvironments())
                .addAllResources(autoRegistrationProperties.agentAutoRegisterResources())
                .setElasticAgentId(autoRegistrationProperties.agentAutoRegisterElasticAgentId())
                .setElasticPluginId(autoRegistrationProperties.agentAutoRegisterElasticPluginId())
                .build();
    }

    private com.thoughtworks.go.protobufs.registration.AgentMeta toProtobuf(AgentMeta agentMeta) {
        return com.thoughtworks.go.protobufs.registration.AgentMeta.newBuilder()
                .setUuid(agentMeta.getUuid())
                .setHostname(agentMeta.getHostname())
                .setLocation(agentMeta.getLocation())
                .setOperatingSystem(agentMeta.getOperationSystem())
                .setUsableSpace(agentMeta.getUsableSpace())
                .build();
    }

    private String responseBody(CloseableHttpResponse response) throws IOException {
        try (InputStream is = response.getEntity() == null ? new NullInputStream(0) : response.getEntity().getContent()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private void setContentType(AbstractHttpMessage request) {
        request.setHeader("Content-Type", "application/x-protobuf");
    }

}
