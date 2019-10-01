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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class AgentAutoRegistrationProperties {
    static final String AGENT_AUTO_REGISTER_KEY = "agent.auto.register.key";
    static final String AGENT_AUTO_REGISTER_RESOURCES = "agent.auto.register.resources";
    static final String AGENT_AUTO_REGISTER_ENVIRONMENTS = "agent.auto.register.environments";
    static final String AGENT_AUTO_REGISTER_HOSTNAME = "agent.auto.register.hostname";
    static final String AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID = "agent.auto.register.elasticAgent.pluginId";
    static final String AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID = "agent.auto.register.elasticAgent.agentId";


    private final File configFile;
    private final Properties properties;

    public AgentAutoRegistrationProperties(File config) {
        this(config, new Properties());
    }

    public AgentAutoRegistrationProperties(File config, Properties properties) {
        this.configFile = config;
        this.properties = properties;
    }

    private boolean exist() {
        return configFile.exists();
    }


    public boolean isElastic() {
        return exist() && !isBlank(agentAutoRegisterElasticPluginId());
    }

    public String agentAutoRegisterKey() {
        return getProperty(AGENT_AUTO_REGISTER_KEY, "");
    }

    public List<String> agentAutoRegisterResources() {
        return Arrays.stream(StringUtils.split(getProperty(AGENT_AUTO_REGISTER_RESOURCES, ""), ",")).map(String::trim).collect(Collectors.toList());
    }

    public List<String> agentAutoRegisterEnvironments() {
        return Arrays.stream(StringUtils.split(getProperty(AGENT_AUTO_REGISTER_ENVIRONMENTS, ""), ",")).map(String::trim).collect(Collectors.toList());
    }

    public String agentAutoRegisterElasticPluginId() {
        return getProperty(AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID, "");
    }

    public String agentAutoRegisterElasticAgentId() {
        return getProperty(AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID, "");
    }

    public String agentAutoRegisterHostname() {
        return getProperty(AGENT_AUTO_REGISTER_HOSTNAME, "");
    }

    private String getProperty(String property, String defaultValue) {
        return properties().getProperty(property, defaultValue);
    }

    private Properties properties() {
        if (this.properties.isEmpty()) {
            loadProperties();
        }
        return this.properties;
    }

    private void loadProperties() {
        try {
            this.properties.clear();
            this.properties.load(reader());
        } catch (IOException e) {
            log.debug("[Agent Auto Registration] Unable to load agent auto register properties file. This agent will not auto-register.", e);
        }
    }

    private StringReader reader() throws IOException {
        return new StringReader(FileUtils.readFileToString(configFile, UTF_8));
    }

}
