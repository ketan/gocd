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
import com.thoughtworks.go.agent.cli.AgentBootstrapperArgs;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.*;
import java.util.Collections;
import java.util.List;

@Component
public class AgentNetworkInterface {
    private final AgentBootstrapperArgs bootstrapperArgs;
    @Getter(lazy = true)
    private final List<NetworkInterface> localInterfaces = findLocalInterfaces();
    @Getter(lazy = true)
    private final String ipUsedToConnectWithServer = findIpUsedToConnectWithServer();
    @Getter(lazy = true)
    private final String hostname = detectLocalHostName();

    @Autowired
    public AgentNetworkInterface(AgentBootstrapperArgs bootstrapperArgs) {
        this.bootstrapperArgs = bootstrapperArgs;
    }

    private String findIpUsedToConnectWithServer() {
        try {
            URL url = bootstrapperArgs.getServerUrl();
            int port = url.getPort();
            if (port == -1) {
                port = url.getDefaultPort();
            }
            try (Socket socket = new Socket(url.getHost(), port)) {
                return socket.getLocalAddress().getHostAddress();
            }
        } catch (Exception e) {
            return getFirstLocalNonLoopbackIpAddress();
        }
    }

    private String getFirstLocalNonLoopbackIpAddress() {
        return getLocalInterfaces().stream()
                .flatMap(NetworkInterface::inetAddresses)
                .filter(address -> address instanceof Inet4Address)
                .filter(address -> !address.isLoopbackAddress())
                .map(InetAddress::getHostAddress)
                .sorted()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to get non-loopback local ip address!"));
    }


    private List<NetworkInterface> findLocalInterfaces() {
        //This must be done only once
        //This method call is extremely slow on JDK 1.7 + Windows combination
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            throw new RuntimeException("Could not retrieve local network interfaces", e);
        }
    }


    private String detectLocalHostName() {
        try {
            return ContextUtil.getLocalHostName();
        } catch (UnknownHostException | SocketException e) {
            return "localhost";
        }
    }
}
