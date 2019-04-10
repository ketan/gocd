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

package com.thoughtworks.go.api.agentservices;

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.agentservices.representers.AgentManifestRepresenter;
import com.thoughtworks.go.api.agentservices.representers.AgentRegisterPayload;
import com.thoughtworks.go.api.base.JsonOutputWriter;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.domain.InputStreamSrc;
import com.thoughtworks.go.domain.JarDetector;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static spark.Spark.*;

@Component
@Slf4j
public class AgentController extends ApiController implements SparkSpringController {

    private final Map<String, Handler> handlers;

    @Getter(lazy = true)
    private final Mac mac = createHmac();

    private final GoConfigService goConfigService;
    private final AgentService agentService;

    @Autowired
    public AgentController(SystemEnvironment systemEnvironment, PluginsZip pluginsZip, GoConfigService goConfigService, AgentService agentService) throws IOException {
        super(ApiVersion.v1);
        this.goConfigService = goConfigService;
        this.agentService = agentService;
        this.handlers = new TreeMap<>(ImmutableMap.<String, Handler>builder()
                .put("agent.jar", new AgentJarHandler(JarDetector.create(systemEnvironment, "agent.jar")))
                .put("agent-launcher.jar", new AgentLauncherHandler(JarDetector.create(systemEnvironment, "agent-launcher.jar")))
                .put("tfs-impl.jar", new TfsImplHandler(JarDetector.create(systemEnvironment, "tfs-impl-14.jar")))
                .put("agent-plugins.zip", new AgentPluginsHandler(pluginsZip, systemEnvironment))
                .build());
    }

    @Override
    public String controllerBasePath() {
        return "/api/agent_services";
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            this.handlers.forEach((jar, handler) ->
                    get("/" + jar, handler.route())
            );

            before("/manifest", mimeType, this::setContentType);
            get("/manifest", mimeType, this::manifest);

            before("/token", mimeType, this::verifyContentType);
            before("/token", mimeType, this::setContentType);
            post("/token", mimeType, this::generateToken);

            before("/register", mimeType, this::verifyContentType);
            before("/register", mimeType, this::setContentType);
            post("/register", mimeType, this::register);
        });
    }

    private String generateToken(Request request, Response response) throws Exception {
        Map<String, Object> map = this.readRequestBodyAsJSON(request);
        Object uuid = map.get("uuid");
        if (uuid instanceof String) {
            String token = computeToken((String) uuid);
            return JsonOutputWriter.OBJECT_MAPPER.writeValueAsString(Collections.singletonMap("token", token));
        } else {
            throw new BadRequestException("UUID must be a string!");
        }
    }

    private String register(Request request, Response response) throws IOException {
        String agentGUID = request.headers("X-Agent-GUID");
        String token = request.headers("Authorization");
        String autoRegisterKey = request.headers("X-Auto-Register-Key");

        AgentRegisterPayload payload = JsonOutputWriter.OBJECT_MAPPER.readValue(request.body(), AgentRegisterPayload.class);
        payload.setIpAddress(request.ip());

        payload.bombIfInvalid();

        denyRegistrationIfTokenDoesNotMatch(agentGUID, token, payload);
        denyRegistrationIfProvidedAutoRegisterKeyDoesNotMatch(autoRegisterKey, payload);
        denyRegistrationIfAutoRegisterKeyIsNotProvidedForAnElasticAgent(payload, autoRegisterKey);
        denyRegistrationIfElasticAgentIsAlreadyRegistered(payload);

        Agent agent = payload.toAgent();
        agent.validate();
        bombIfAgentHasErrors(agent, payload);

        if (serverAutoregisterKey().equals(autoRegisterKey)) {
            agentService.register(agent);
            bombIfAgentHasErrors(agent, payload);
            return MessageJson.create("Welcome!");
        }

        AgentRuntimeInfo agentRuntimeInfo = payload.toRuntimeInfo(agentService.isRegistered(payload.getUuid()));
        Registration registration = agentService.requestRegistration(agentRuntimeInfo);
        if (registration.isValid()) {
            return MessageJson.create("Welcome!");
        } else {
            response.status(202);
            return MessageJson.create("Agent is in pending state, waiting for approval from a GoCD server administrator.");
        }
    }

    private void bombIfAgentHasErrors(Agent agent, AgentRegisterPayload payload) {
        if (agent.hasErrors()) {
            String errors = join("\n", agent.errors().getAll());
            log.error("Rejecting request for registration because of validation errors: {}. The remote agent was identified by: {}", errors, payload);
            throw new BadRequestException(format("Rejecting request for registration because of validation errors: %s", errors));
        }
    }

    private void denyRegistrationIfAutoRegisterKeyIsNotProvidedForAnElasticAgent(AgentRegisterPayload payload, String autoRegisterKey) {
        if (payload.getAutoRegister().isElastic() && isBlank(autoRegisterKey)) {
            log.error("Rejecting request for registration because the auto register key must be provided for an elastic agent!. The remote agent was identified by: {}", payload);
            throw new NotAuthorizedException("Auto register key must be provided!");
        }
    }

    private void denyRegistrationIfElasticAgentIsAlreadyRegistered(AgentRegisterPayload payload) {
        if (payload.getAutoRegister().isElastic() && agentService.findElasticAgent(payload.getAutoRegister().getElasticAgentId(), payload.getAutoRegister().getElasticPluginId()) != null) {
            log.error("Rejecting request for registration because elastic agent with id {} is already registered. The remote agent was identified by: {}", payload.getAutoRegister().getElasticAgentId(), payload);
            throw new BadRequestException("Duplicate Elastic agent Id used to register elastic agent.");
        }
    }

    private void denyRegistrationIfTokenDoesNotMatch(String agentGUID, String token, AgentRegisterPayload payload) {
        if (!computeToken(agentGUID).equals(token)) {
            log.error("Rejecting request for registration because the token is invalid. The remote agent was identified by: {}", payload);
            throw new NotAuthorizedException("Invalid token!");
        }
    }

    private void denyRegistrationIfProvidedAutoRegisterKeyDoesNotMatch(String autoRegisterKey, AgentRegisterPayload payload) {
        if (isNotBlank(autoRegisterKey) && !autoRegisterKey.equals(serverAutoregisterKey())) {
            log.error("Rejecting request for registration because the auto register key is invalid. The remote agent was identified by: {}", payload);
            throw new NotAuthorizedException("Invalid auto register key!");
        }
    }

    private String serverAutoregisterKey() {
        return goConfigService.serverConfig().getAgentAutoRegisterKey();
    }

    private String manifest(Request request, Response response) throws IOException {
        String etag = DigestUtils.sha256Hex(StringUtils.joinWith("/", this.handlers.keySet()));

        if (fresh(request, etag)) {
            return notModified(response);
        } else {
            writerForTopLevelObject(request, response, outputWriter -> AgentManifestRepresenter.toJSON(outputWriter, this.handlers));
        }

        return NOTHING;
    }

    private String sendJarIfChecksumMatch(Request request, Response response, String etag, InputStreamSrc jarSrc) throws IOException {
        if (fresh(request, etag)) {
            return notModified(response);
        } else {
            sendFile(jarSrc, etag, response);
        }
        return NOTHING;
    }

    private void sendFile(InputStreamSrc input, String etag, Response response) throws IOException {
        response.type("application/octet-stream");
        setEtagHeader(response, etag);
        try (InputStream in = input.invoke()) {
            IOUtils.copy(in, response.raw().getOutputStream());
        }
    }


    // creating macs is expensive, but they cannot be shared either, so we synchronize access so it's only computing one
    // mac at any given time
    synchronized String computeToken(String uuid) {
        return Base64.getEncoder().encodeToString(getMac().doFinal(uuid.getBytes(StandardCharsets.UTF_8)));
    }

    private Mac createHmac() {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(goConfigService.serverConfig().getTokenGenerationKey().getBytes(), "HmacSHA256");
            mac.init(secretKey);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private class AgentJarHandler extends Handler {
        AgentJarHandler(InputStreamSrc src) {
            super(src);
        }

        @Override
        public Route route() {
            return (request, response) -> sendJarIfChecksumMatch(request, response, getChecksum(), this.src);
        }
    }

    private class AgentLauncherHandler extends Handler {
        AgentLauncherHandler(InputStreamSrc src) {
            super(src);
        }

        @Override
        public Route route() {
            return (request, response) -> sendJarIfChecksumMatch(request, response, getChecksum(), this.src);
        }

    }

    private class TfsImplHandler extends Handler {
        TfsImplHandler(InputStreamSrc src) {
            super(src);
        }

        @Override
        public Route route() {
            return (request, response) -> sendJarIfChecksumMatch(request, response, getChecksum(), this.src);
        }

    }

    private class AgentPluginsHandler extends Handler {
        private final PluginsZip pluginsZip;

        AgentPluginsHandler(PluginsZip pluginsZip, SystemEnvironment systemEnvironment) throws IOException {
            super(JarDetector.createFromFile(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)));
            this.pluginsZip = pluginsZip;
        }

        @Override
        public Route route() {
            return (request, response) -> sendJarIfChecksumMatch(request, response, getChecksum(), this.src);
        }

        @Override
        public String getChecksum() {
            return pluginsZip.sha256();
        }
    }

}
