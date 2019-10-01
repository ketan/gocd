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

package com.thoughtworks.go.agent.registration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAutoRegistrationPropertiesTest {
    private File configFile;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws IOException {
        configFile = tempFolder.resolve("autoregister.properties").toFile();
        configFile.createNewFile();
    }

    @Test
    void shouldReturnAgentAutoRegisterPropertiesIfPresent() throws Exception {
        Properties properties = new Properties();

        properties.put(AgentAutoRegistrationProperties.AGENT_AUTO_REGISTER_KEY, "foo");
        properties.put(AgentAutoRegistrationProperties.AGENT_AUTO_REGISTER_RESOURCES, "foo, zoo");
        properties.put(AgentAutoRegistrationProperties.AGENT_AUTO_REGISTER_ENVIRONMENTS, "foo, bar");
        properties.put(AgentAutoRegistrationProperties.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");
        properties.store(new FileOutputStream(configFile), "");

        AgentAutoRegistrationProperties reader = new AgentAutoRegistrationProperties(configFile);
        assertThat(reader.agentAutoRegisterKey()).isEqualTo("foo");
        assertThat(reader.agentAutoRegisterResources()).hasSize(2).contains("foo", "zoo");
        assertThat(reader.agentAutoRegisterEnvironments()).hasSize(2).contains("foo", "bar");
        assertThat(reader.agentAutoRegisterHostname()).isEqualTo("agent01.example.com");
    }

    @Test
    void shouldReturnEmptyStringIfPropertiesNotPresent() {
        AgentAutoRegistrationProperties reader = new AgentAutoRegistrationProperties(configFile);
        assertThat(reader.agentAutoRegisterKey()).isEmpty();
        assertThat(reader.agentAutoRegisterResources()).isEmpty();
        assertThat(reader.agentAutoRegisterEnvironments()).isEmpty();
        assertThat(reader.agentAutoRegisterHostname()).isEmpty();
    }

}