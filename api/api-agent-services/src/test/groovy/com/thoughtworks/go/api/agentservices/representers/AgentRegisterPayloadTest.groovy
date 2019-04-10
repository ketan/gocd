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

package com.thoughtworks.go.api.agentservices.representers

import com.thoughtworks.go.config.exceptions.BadRequestException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThatCode

class AgentRegisterPayloadTest {

  @Nested
  class Validate {
    @Test
    void "should allow null ip address"() {
      def payload = new AgentRegisterPayload()
        .setIpAddress(null)
        .setUuid(UUID.randomUUID().toString())
      assertThatCode({ payload.bombIfInvalid() })
        .doesNotThrowAnyException()
    }

    @Test
    void "should bomb when agent ip address is blank"() {
      def payload = new AgentRegisterPayload().setIpAddress("      ")
      assertThatCode({ payload.bombIfInvalid() })
        .isInstanceOf(BadRequestException.class)
        .hasMessage("IpAddress cannot be empty if it is present.")
    }

    @Test
    void "should bomb when agent ip address is invalid"() {
      def payload = new AgentRegisterPayload().setIpAddress("127.0.0.bar")
      assertThatCode({ payload.bombIfInvalid() })
        .isInstanceOf(BadRequestException.class)
        .hasMessage("'127.0.0.bar' is an invalid IP address.")
    }

    @Test
    void "should bomb when agent uuid is null"() {
      def payload = new AgentRegisterPayload()
        .setIpAddress("127.0.0.1")
        .setUuid(null)

      assertThatCode({ payload.bombIfInvalid() })
        .isInstanceOf(BadRequestException.class)
        .hasMessage("UUID cannot be empty.")
    }

    @Test
    void "should bomb when agent uuid is empty"() {
      def payload = new AgentRegisterPayload()
        .setIpAddress("127.0.0.1")
        .setUuid("      ")

      assertThatCode({ payload.bombIfInvalid() })
        .isInstanceOf(BadRequestException.class)
        .hasMessage("UUID cannot be empty.")
    }

    @Test
    void "should allow resources for non elastic agent"() {
      def payload = new AgentRegisterPayload()
        .setIpAddress("127.0.0.1")
        .setUuid(UUID.randomUUID().toString())
        .setAutoRegister(
          new AgentRegisterPayload.AutoRegisterPayload()
            .setResources(List.of("linux", "firefox"))
        )

      assertThatCode({ payload.bombIfInvalid() })
        .doesNotThrowAnyException()
    }

    @Test
    void "should bomb when resources are specified for an elastic agent"() {
      def payload = new AgentRegisterPayload()
        .setIpAddress("127.0.0.1")
        .setUuid(UUID.randomUUID().toString())
        .setAutoRegister(
          new AgentRegisterPayload.AutoRegisterPayload()
            .setElasticAgentId(UUID.randomUUID().toString())
            .setElasticPluginId("cd.go.elastic.docker")
            .setResources(List.of("linux", "firefox"))
        )

      assertThatCode({ payload.bombIfInvalid() })
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Elastic agents cannot have resources.")
    }

    @Test
    void "should bomb when elastic agent id provided but elastic plugin id not"() {
      def payload = new AgentRegisterPayload()
        .setIpAddress("127.0.0.1")
        .setUuid(UUID.randomUUID().toString())
        .setAutoRegister(
          new AgentRegisterPayload.AutoRegisterPayload()
            .setElasticAgentId(UUID.randomUUID().toString())
        )

      assertThatCode({ payload.bombIfInvalid() })
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Elastic agents must submit both elastic_agent_id and elastic_plugin_id.")
    }

    @Test
    void "should bomb when elastic plugin id provided but elastic agent id not"() {
      def payload = new AgentRegisterPayload()
        .setIpAddress("127.0.0.1")
        .setUuid(UUID.randomUUID().toString())
        .setAutoRegister(
          new AgentRegisterPayload.AutoRegisterPayload()
            .setElasticPluginId("cd.go.elastic.docker")
        )

      assertThatCode({ payload.bombIfInvalid() })
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Elastic agents must submit both elastic_agent_id and elastic_plugin_id.")
    }
  }
}