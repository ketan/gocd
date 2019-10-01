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

package com.thoughtworks.go.agent.system;

import ch.qos.logback.core.util.ContextUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.net.SocketException;
import java.net.UnknownHostException;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "go.agent")
public class GoAgentProperties {
    private File configDir;
    private String[] sslProtocol;
    private String[] cipherSuites;
    private StatusAPI statusApi;


    public File autoRegisterProperties() {
        return new File(configDir, "autoregister.properties");
    }

    public static String getLocalHostName() {
        try {
            return ContextUtil.getLocalHostName();
        } catch (UnknownHostException | SocketException e) {
            return "localhost";
        }
    }

    @Getter
    @Setter
    @ToString
    public static class StatusAPI {
        private boolean enabled = true;

        private String bindHost = "localhost";

        private int bindPort = 8152;
    }
}
