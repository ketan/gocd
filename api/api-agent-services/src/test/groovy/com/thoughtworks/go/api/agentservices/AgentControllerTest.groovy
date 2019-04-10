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

package com.thoughtworks.go.api.agentservices

import com.thoughtworks.go.config.Agent
import com.thoughtworks.go.config.ServerConfig
import com.thoughtworks.go.domain.AgentInstance
import com.thoughtworks.go.plugin.infra.commons.PluginsZip
import com.thoughtworks.go.security.Registration
import com.thoughtworks.go.server.service.AgentRuntimeInfo
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.GoConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.util.SecureRandom
import com.thoughtworks.go.util.SystemEnvironment
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock

import java.util.stream.Stream

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class AgentControllerTest implements ControllerTrait<AgentController> {

  @Mock
  SystemEnvironment systemEnvironment
  @Mock
  PluginsZip pluginsZip
  @Mock
  GoConfigService goConfigService
  @Mock
  AgentService agentService

  @BeforeEach
  void setUp() {
    initMocks(this)
    when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn("agent-plugins.zip")
    when(pluginsZip.sha256()).thenAnswer({
      return hexOfFile(new File("agent-plugins.zip"))
    })

    urls().forEach({ Arguments eachArgSet ->
      new File(eachArgSet.get()[1] as String).text = SecureRandom.hex()
    })
  }

  @AfterEach
  void tearDown() {
    urls().forEach({ Arguments eachArgSet ->
      new File(eachArgSet.get()[1] as String).delete()
    })
  }

  @Override
  AgentController createControllerInstance() {
    return new AgentController(systemEnvironment, pluginsZip, goConfigService, agentService)
  }

  @Nested
  class StaticJars {
    @MethodSource("urls")
    @ParameterizedTest
    void 'should GET jar'(String url, String nameOfJar) {
      get(controller.controllerPath(url))

      assertThatResponse()
        .isOk()
        .hasContentType("application/octet-stream")
        .hasEtag($/"${hexOfFile(new File(nameOfJar))}"/$)
        .hasBody(new File(nameOfJar).bytes)
    }

    @ParameterizedTest
    @MethodSource("urls")
    void 'GET should render 304 if etag has not changed'(String url, String nameOfJar) {
      get(controller.controllerPath(url), ['If-None-Match': $/"${hexOfFile(new File(nameOfJar))}"/$])

      assertThatResponse()
        .isNotModified()
        .hasNoBody()
    }

    @ParameterizedTest
    @MethodSource("urls")
    void 'GET should render jar if etag has changed'(String url, String nameOfJar) {
      get(controller.controllerPath(url), ['If-None-Match': '"blah"'])

      assertThatResponse()
        .isOk()
        .hasContentType("application/octet-stream")
        .hasEtag($/"${hexOfFile(new File(nameOfJar))}"/$)
        .hasBody(new File(nameOfJar).bytes)
    }

    static Stream<Arguments> urls() {
      return AgentControllerTest.urls()
    }
  }

  @Nested
  class Manifest {
    @Test
    void 'should get manifest'() {
      getWithApiHeader(controller.controllerPath('/manifest'))

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
    }
  }

  @Nested
  class Token {
    @Test
    void 'should return a token corresponding to the UUID'() {
      def config = new ServerConfig()
      config.tokenGenerationKey = "blah"
      when(goConfigService.serverConfig()).thenReturn(config)
      postWithApiHeader(controller.controllerPath("token"), [
        uuid: "some-uuid"
      ])

      assertThatResponse()
        .isOk()
        .hasJsonBody([
          token: 'Jb+nMCtbut7lF3ZfAGyMWDgC6kH+ki8q0gyBPPmRJ5g='
        ])
    }
  }

  @Nested
  class RequestRegistration {
    private serverConfig

    @BeforeEach
    void setUp() {
      serverConfig = new ServerConfig()
      serverConfig.ensureTokenGenerationKeyExists()
      serverConfig.ensureAgentAutoregisterKeyExists()
      when(goConfigService.serverConfig()).thenReturn(serverConfig)
    }

    @Test
    void 'should register an agent'() {
      def agentUUID = UUID.randomUUID().toString()
      def token = controller.computeToken(agentUUID)

      def headers = [
        'X-Agent-GUID'       : agentUUID,
        'Authorization'      : token,
        'X-Auto-Register-Key': serverConfig.agentAutoRegisterKey
      ]

      postWithApiHeader(controller.controllerPath("register"), headers, autoRegisterPayload(agentUUID))

      verify(agentService).register(any(Agent))
      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Welcome!")
    }

    @Test
    void 'should accept registration when agent auto register key is not provided agent is not previously registered'() {
      def agentUUID = UUID.randomUUID().toString()
      def token = controller.computeToken(agentUUID)

      def headers = [
        'X-Agent-GUID' : agentUUID,
        'Authorization': token
      ]

      when(agentService.requestRegistration(any(AgentRuntimeInfo))).thenReturn(Registration.createNullPrivateKeyEntry())
      when(agentService.isRegistered(agentUUID)).thenReturn(false)

      postWithApiHeader(controller.controllerPath("register"), headers, autoRegisterPayload(agentUUID))

      verify(agentService, never()).register(any(Agent))
      verify(agentService).requestRegistration(any(AgentRuntimeInfo))
      assertThatResponse()
        .isAccepted()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Agent is in pending state, waiting for approval from a GoCD server administrator.")
    }

    @Test
    void 'should accept registration when agent auto register key is not provided and agent is previously registered'() {
      def agentUUID = UUID.randomUUID().toString()
      def token = controller.computeToken(agentUUID)

      def headers = [
        'X-Agent-GUID' : agentUUID,
        'Authorization': token
      ]

      def registration = mock(Registration)
      when(registration.isValid()).thenReturn(true)
      when(agentService.requestRegistration(any(AgentRuntimeInfo))).thenReturn(registration)
      when(agentService.isRegistered(agentUUID)).thenReturn(true)

      postWithApiHeader(controller.controllerPath("register"), headers, autoRegisterPayload(agentUUID))

      verify(agentService, never()).register(any(Agent))
      verify(agentService).requestRegistration(any(AgentRuntimeInfo))
      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Welcome!")
    }

    @Test
    void 'should deny registration when agent submits a bad token'() {
      def agentUUID = UUID.randomUUID().toString()
      def headers = [
        'X-Agent-GUID'       : agentUUID,
        'Authorization'      : "bad-token",
        'X-Auto-Register-Key': serverConfig.agentAutoRegisterKey
      ]

      postWithApiHeader(controller.controllerPath("register"), headers, autoRegisterPayload(agentUUID))

      verifyZeroInteractions(agentService)
      assertThatResponse()
        .isUnauthorized()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Invalid token!")
    }

    @Test
    void 'should deny registration when agent submits a bad auto register key'() {
      def agentUUID = UUID.randomUUID().toString()
      def token = controller.computeToken(agentUUID)
      def headers = [
        'X-Agent-GUID'       : agentUUID,
        'Authorization'      : token,
        'X-Auto-Register-Key': "bad-key"
      ]

      postWithApiHeader(controller.controllerPath("register"), headers, autoRegisterPayload(agentUUID))

      verifyZeroInteractions(agentService)
      assertThatResponse()
        .isUnauthorized()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Invalid auto register key!")
    }

    @Test
    void 'should deny registration for agent when provided agent information is invalid'() {
      def agentUUID = UUID.randomUUID().toString()
      def token = controller.computeToken(agentUUID)
      def headers = [
        'X-Agent-GUID' : agentUUID,
        'Authorization': token
      ]

      def payloadWithoutAgentUUID = autoRegisterPayload(null)
      postWithApiHeader(controller.controllerPath("register"), headers, payloadWithoutAgentUUID)

      verify(agentService, atMostOnce()).findElasticAgent(anyString(), anyString())
      verify(agentService, never()).register(any())
      assertThatResponse()
        .isBadRequest()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("UUID cannot be empty.")
    }

    @Nested
    class ElasticAgent {
      @Test
      void 'should deny registration for elastic agent when agent with same id is already registered'() {
        def agentUUID = UUID.randomUUID().toString()
        def token = controller.computeToken(agentUUID)
        def payload = autoRegisterPayload(agentUUID, true)
        def headers = [
          'X-Agent-GUID'       : agentUUID,
          'Authorization'      : token,
          'X-Auto-Register-Key': serverConfig.agentAutoRegisterKey
        ]

        when(agentService.findElasticAgent(anyString(), anyString())).thenReturn(mock(AgentInstance))
        postWithApiHeader(controller.controllerPath("register"), headers, payload)

        verify(agentService, never()).register(any())
        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Duplicate Elastic agent Id used to register elastic agent.")
      }

      @Test
      void 'should deny registration for elastic agent when elastic agent id is missing'() {
        def agentUUID = UUID.randomUUID().toString()
        def token = controller.computeToken(agentUUID)
        def payload = autoRegisterPayload(agentUUID, true)
        def headers = [
          'X-Agent-GUID'       : agentUUID,
          'Authorization'      : token,
          'X-Auto-Register-Key': serverConfig.agentAutoRegisterKey
        ]

        payload.auto_register.remove("elastic_agent_id")
        postWithApiHeader(controller.controllerPath("register"), headers, payload)

        verify(agentService, never()).register(any())
        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Elastic agents must submit both elastic_agent_id and elastic_plugin_id.")
      }

      @Test
      void 'should deny registration for elastic agent when elastic plugin id is missing'() {
        def agentUUID = UUID.randomUUID().toString()
        def token = controller.computeToken(agentUUID)
        def payload = autoRegisterPayload(agentUUID, true)
        def headers = [
          'X-Agent-GUID'       : agentUUID,
          'Authorization'      : token,
          'X-Auto-Register-Key': serverConfig.agentAutoRegisterKey
        ]

        payload.auto_register.remove("elastic_plugin_id")
        postWithApiHeader(controller.controllerPath("register"), headers, payload)

        verify(agentService, never()).register(any())
        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Elastic agents must submit both elastic_agent_id and elastic_plugin_id.")
      }

      @Test
      void 'should deny registration for an elastic agent when auto register key is not provided'() {
        def agentUUID = UUID.randomUUID().toString()
        def token = controller.computeToken(agentUUID)
        def headers = [
          'X-Agent-GUID' : agentUUID,
          'Authorization': token
        ]

        postWithApiHeader(controller.controllerPath("register"), headers, autoRegisterPayload(agentUUID, true))

        verifyZeroInteractions(agentService)
        assertThatResponse()
          .isUnauthorized()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Auto register key must be provided!")
      }
    }

    private def autoRegisterPayload(String agentUUID, boolean isElastic = false) {
      def autoRegister = [
        environments: ['prod'],
        resources   : ['linux', 'firefox'],
        hostname    : 'agent01.example.com'
      ]

      if (isElastic) {
        autoRegister.remove("resources")
        autoRegister = autoRegister + [
          elastic_agent_id : UUID.randomUUID().toString(),
          elastic_plugin_id: "cd.go.elastic.docker"
        ]
      }

      return [
        hostname        : SecureRandom.hex(),
        uuid            : agentUUID,

        auto_register   : autoRegister,

        location        : '/go/agent01',
        usable_space    : 10000000L,
        operating_system: 'linux'
      ]
    }
  }

  private static String hexOfFile(File file) {
    DigestUtils.sha256Hex(file.text)
  }

  static Stream<Arguments> urls() {
    return Stream.of(
      Arguments.of("agent.jar", "agent.jar"),
      Arguments.of("agent-launcher.jar", "agent-launcher.jar"),
      Arguments.of("tfs-impl.jar", "tfs-impl-14.jar"),
      Arguments.of("agent-plugins.zip", "agent-plugins.zip")
    )
  }
}
