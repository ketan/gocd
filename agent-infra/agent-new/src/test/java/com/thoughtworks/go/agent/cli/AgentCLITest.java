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

package com.thoughtworks.go.agent.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AgentCLITest {

    private ByteArrayOutputStream errorStream;
    private AgentCLI agentCLI;
    private AgentCLI.SystemExitter exitter;

    @BeforeEach
    void setUp() {
        errorStream = new ByteArrayOutputStream();
        exitter = status -> {
            throw new ExitException(status);
        };
        agentCLI = new AgentCLI(new PrintStream(errorStream), exitter);
    }


    @Test
    void shouldDieIfNoArguments() {
        assertThatCode(() -> agentCLI.parse())
                .isInstanceOf(ExitException.class)
                .satisfies(o -> assertThat(((ExitException) o).getStatus()).isEqualTo(1));

        assertThat(errorStream.toString()).contains("The following option is required: [-serverUrl]");
        assertThat(errorStream.toString()).contains("Usage: java -jar agent-bootstrapper.jar");
    }

    @Test
    void serverURLMustBeAValidURL() throws Exception {
        assertThatCode(() -> agentCLI.parse("-serverUrl", "foobar"))
                .isInstanceOf(ExitException.class)
                .satisfies(o -> assertThat(((ExitException) o).getStatus()).isEqualTo(1));

        assertThat(errorStream.toString()).contains("-serverUrl is not a valid url");
        assertThat(errorStream.toString()).contains("Usage: java -jar agent-bootstrapper.jar");
    }

    @Test
    void shouldPassIfCorrectArgumentsAreProvided() throws Exception {
        AgentBootstrapperArgs agentBootstrapperArgs = agentCLI.parse("-serverUrl", "https://go.example.com:8154/go", "-sslVerificationMode", "NONE");

        assertThat(agentBootstrapperArgs.getServerUrl()).isEqualTo(new URL("https://go.example.com:8154/go"));
        assertThat(agentBootstrapperArgs.getSslVerificationMode()).isEqualTo(AgentBootstrapperArgs.SslMode.NONE);
    }

    @Test
    void shouldRaisExceptionWhenInvalidSslModeIsPassed() throws Exception {
        assertThatCode(() -> agentCLI.parse("-serverUrl", "https://go.example.com:8154/go", "-sslVerificationMode", "FOOBAR"))
                .isInstanceOf(ExitException.class)
                .satisfies(o -> assertThat(((ExitException) o).getStatus()).isEqualTo(1));

        assertThat(errorStream.toString()).contains("Invalid value for -sslVerificationMode parameter. Allowed values:[FULL, NONE, NO_VERIFY_HOST]");
        assertThat(errorStream.toString()).contains("Usage: java -jar agent-bootstrapper.jar");
    }

    @Test
    void shouldDefaultsTheSslModeToNONEWhenNotSpecified() {
        AgentBootstrapperArgs agentBootstrapperArgs = agentCLI.parse("-serverUrl", "https://go.example.com/go");
        assertThat(agentBootstrapperArgs.getSslVerificationMode()).isEqualTo(AgentBootstrapperArgs.SslMode.NONE);
    }

    @Test
    void printsHelpAndExitsWith0() {
        assertThatCode(() -> agentCLI.parse("-help"))
                .isInstanceOf(ExitException.class)
                .satisfies(o -> assertThat(((ExitException) o).getStatus()).isZero());
    }

    static class ExitException extends RuntimeException {
        private final int status;

        ExitException(int status) {
            this.status = status;
        }

        int getStatus() {
            return status;
        }
    }
}
