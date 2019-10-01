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
import com.google.protobuf.MessageLite;
import com.thoughtworks.go.api.ControllerMethods;
import com.thoughtworks.go.api.agentservices.representers.AgentManifestRepresenter;
import com.thoughtworks.go.api.agentservices.representers.AgentRegisterPayload;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.protobufs.ProtoMessage;
import com.thoughtworks.go.protobufs.registration.AgentMeta;
import com.thoughtworks.go.protobufs.registration.Cookie;
import com.thoughtworks.go.protobufs.registration.Token;
import com.thoughtworks.go.protobufs.registration.UUID;
import com.thoughtworks.go.protobufs.tasks.ProtoExec;
import com.thoughtworks.go.protobufs.tasks.ProtoJobIdentifier;
import com.thoughtworks.go.protobufs.tasks.ProtoPipelineIdentifier;
import com.thoughtworks.go.protobufs.tasks.ProtoStageIdentifier;
import com.thoughtworks.go.protobufs.work.*;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.*;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.messaging.scheduling.WorkAssignments;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.toprotobuf.TaskConverterFactory;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpStatus.*;
import static spark.Spark.*;

@Component
@Slf4j
public class AgentController implements SparkSpringController, ControllerMethods, SparkController {

    private final Map<String, Handler> handlers;

    @Getter(lazy = true)
    private final Mac mac = createHmac();

    private final GoConfigService goConfigService;
    private final AgentService agentService;
    private final WorkAssignments workAssignments;
//    private final String mimeType;

    @Autowired
    public AgentController(SystemEnvironment systemEnvironment,
                           PluginsZip pluginsZip,
                           GoConfigService goConfigService,
                           AgentService agentService,
                           WorkAssignments workAssignments) throws IOException {
//        this.mimeType = "application/x-protobuf";
        this.goConfigService = goConfigService;
        this.agentService = agentService;
        this.workAssignments = workAssignments;
        this.handlers = new TreeMap<>(ImmutableMap.<String, Handler>builder()
                .put("agent.jar", new AgentJarHandler(JarDetector.create(systemEnvironment, "agent.jar")))
                .put("agent-launcher.jar", new AgentLauncherHandler(JarDetector.create(systemEnvironment, "agent-launcher.jar")))
                .put("tfs-impl.jar", new TfsImplHandler(JarDetector.create(systemEnvironment, "tfs-impl-14.jar")))
                .put("agent-plugins.zip", new AgentPluginsHandler(pluginsZip, systemEnvironment))
                .build());
    }

    @Override
    public String controllerBasePath() {
        return "/agent_services";
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            this.handlers.forEach((jar, handler) ->
                    get("/" + jar, handler.route())
            );

            get("/manifest", this::manifest);

            post("/token", this::generateToken);

            post("/register", this::register);
            post("/cookie", this::getCookie);
            post("/work", this::getWork);
        });
    }

    private byte[] generateToken(Request request, Response response) throws Exception {
        UUID uuid = UUID.parseFrom(request.bodyAsBytes());
        String token = computeToken(uuid.getUuid());
        return Token.newBuilder().setToken(token).build().toByteArray();
    }

    private byte[] register(Request request, Response response) throws IOException {
        String agentGUID = request.headers("X-Agent-GUID");
        String token = request.headers("Authorization");
        String autoRegisterKey = request.headers("X-Auto-Register-Key");

        AgentRegisterPayload payload = AgentRegisterPayload.from(request.bodyAsBytes());
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
            return toMessage("Welcome!").toByteArray();
        }

        AgentRuntimeInfo agentRuntimeInfo = payload.toRuntimeInfo(agentService.isRegistered(payload.getUuid()));
        Registration registration = agentService.requestRegistration(agentRuntimeInfo);
        if (registration.isValid()) {
            return toMessage("Welcome!").toByteArray();
        } else {
            response.status(202);
            return toMessage("Agent is in pending state, waiting for approval from a GoCD server administrator.").toByteArray();
        }
    }

    private byte[] getCookie(Request request, Response response) throws Exception {
        AgentMeta agentMeta = AgentMeta.parseFrom(request.bodyAsBytes());
        Agent agent = agentService.findAgentByUUID(agentMeta.getUuid());
        if (agent == null) {
            throw new UnprocessableEntityException(format("Agent with UUID %s does not exist.", agentMeta.getUuid()));
        }

        if (!agent.isEnabled()) {
            throw new BadRequestException(format("Agent with uuid %s is not registered with server yet, waiting for approval from a GoCD server administrator.", agentMeta.getUuid()));
        }

        agent.refreshCookie();
        agentService.saveOrUpdate(agent);

        return Cookie.newBuilder().setCookie(agent.getCookie()).build().toByteArray();
    }

    private byte[] getWork(Request request, Response response) throws IOException {
        String agentCookie = request.headers("X-Agent-Cookie");
        AgentMeta agentMeta = AgentMeta.parseFrom(request.bodyAsBytes());
        AgentIdentifier agentIdentifier = new AgentIdentifier(agentMeta.getHostname(), agentMeta.getIpAddress(), agentMeta.getUuid());
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, agentMeta.getLocation(), agentCookie);

        Work work = workAssignments.getWork(agentRuntimeInfo);
        MessageLite message = toProtobuf(work, response);
        return message.toByteArray();
    }

    private MessageLite toProtobuf(Work work, Response response) {
        if (work instanceof NoWork) {
            response.status(SC_ACCEPTED);
            return toMessage("No work.");
        }

        if (work instanceof DeniedAgentWork) {
            response.status(SC_FORBIDDEN);
            return toMessage("Denied work. Agent is disabled.");
        }

        if (work instanceof UnregisteredAgentWork) {
            response.status(SC_UNAUTHORIZED);
            return toMessage("Agent is not registered.");
        }

        if (work instanceof BuildWork) {
            BuildWork buildWork = (BuildWork) work;
            BuildAssignment assignment = buildWork.getAssignment();
            JobIdentifier jobIdentifier = assignment.getJobIdentifier();
            List<Task> tasks = goConfigService.tasksForJob(jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName());

            return ProtoWork.newBuilder()
                    .setJobIdentifier(toProtobuf(jobIdentifier))
                    .addAllMaterial(toProtobuf(assignment.getMaterialRevisions()))
                    .addAllTask(tasks.stream().map(this::toProtobuf).collect(toList()))
                    .build();
        }

        throw new IllegalArgumentException(format("Work type %s is not supported.", work.getClass().getName()));
    }

    private ProtoJobIdentifier toProtobuf(JobIdentifier jobIdentifier) {
        ProtoPipelineIdentifier pipelineIdentifier = ProtoPipelineIdentifier.newBuilder()
                .setPipelineName(jobIdentifier.getPipelineName())
                .setPipelineCounter(jobIdentifier.getPipelineCounter())
                .build();

        ProtoStageIdentifier stageIdentifier = ProtoStageIdentifier.newBuilder()
                .setStageCounter(Long.parseLong(jobIdentifier.getStageCounter()))
                .setStageName(jobIdentifier.getStageName())
                .setPipelineIdentifier(pipelineIdentifier)
                .build();

        return ProtoJobIdentifier.newBuilder()
                .setJobName(jobIdentifier.getBuildName())
                .setStageIdentifier(stageIdentifier)
                .build();
    }

    private ProtoExec toProtobuf(Task task) {
        return new TaskConverterFactory().toTask(task);
    }

    private List<ProtoMaterialRevision> toProtobuf(MaterialRevisions materialRevisions) {
        return StreamSupport.stream(materialRevisions.spliterator(), false)
                .map(this::toProtobuf)
                .collect(toList());
    }

    private ProtoMaterialRevision toProtobuf(MaterialRevision materialRevision) {
        ProtoMaterialRevision.Builder builder = ProtoMaterialRevision.newBuilder();
        if (materialRevision.getMaterial() instanceof GitMaterial) {
            builder.setGit(toProtobuf(materialRevision, (GitMaterial) materialRevision.getMaterial()));
        }
        return builder.build();
    }

    private ProtoGitMaterialRevision toProtobuf(MaterialRevision materialRevision, GitMaterial material) {
        return ProtoGitMaterialRevision.newBuilder()
                .setConfig(toProtobuf(material))
                .setPrevious(toProtobuf(materialRevision.getModifications().first()))
                .setLatest(toProtobuf(materialRevision.getModifications().last()))
                .build();
    }

    private ProtoGitMaterialConfig toProtobuf(GitMaterial material) {
        ProtoGitMaterialConfig.Builder builder = ProtoGitMaterialConfig.newBuilder()
                .setUrl(material.urlForCommandLine())
                .setBranch(material.getBranch())
                .setShallow(material.isShallowClone());

        if (isNotBlank(material.getUserName())) {
            builder.setUsername(material.getUsername());
        }

        if (isNotBlank(material.getPassword())) {
            builder.setPassword(material.getPassword());
        }
        return builder.build();
    }

    private ProtoGitRevision toProtobuf(Modification modification) {
        return ProtoGitRevision.newBuilder().setSha(modification.getRevision()).build();
    }

    private MessageLite toMessage(String s) {
        return ProtoMessage
                .newBuilder()
                .setMessage(s)
                .build();
    }

    private void bombIfAgentHasErrors(Agent agent, AgentRegisterPayload payload) {
        if (agent.hasErrors()) {
            String errors = join("\n", agent.errors().getAll());
            log.error("Rejecting request for registration because of validation errors: {}. The remote agent was identified by: {}", errors, payload);
            throw new BadRequestException(format("Rejecting request for registration because of validation errors: %s", errors));
        }
    }

    private void denyRegistrationIfAutoRegisterKeyIsNotProvidedForAnElasticAgent(AgentRegisterPayload payload,
                                                                                 String autoRegisterKey) {
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

    private void denyRegistrationIfProvidedAutoRegisterKeyDoesNotMatch(String autoRegisterKey,
                                                                       AgentRegisterPayload payload) {
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

    private String sendJarIfChecksumMatch(Request request,
                                          Response response,
                                          String etag,
                                          InputStreamSrc jarSrc) throws IOException {
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
