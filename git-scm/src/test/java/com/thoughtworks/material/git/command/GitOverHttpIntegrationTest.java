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

package com.thoughtworks.material.git.command;

import com.thoughtworks.material.git.command.args.GitCommandId;
import com.thoughtworks.material.git.command.config.GitConfig;
import com.thoughtworks.material.git.command.exceptions.GitCommandExecutionException;
import com.thoughtworks.material.git.command.executors.GitCommandResult;
import com.thoughtworks.material.git.command.executors.GitProcessExecutor;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.http.server.GitServlet;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import javax.servlet.DispatcherType;
import java.io.File;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnableRuleMigrationSupport
public class GitOverHttpIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File gitRepositoriesRoot;
    private Server server;

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty(GitProcessExecutor.SSH_CLI_JAR_FILE_PATH, "testdata/gen/ssh-cli.jar");

        File basedir = temporaryFolder.newFolder("source-root");
        new GitRepository(basedir).initialize();

        gitRepositoriesRoot = temporaryFolder.newFolder("git-root");
        FileUtils.copyDirectory(basedir, new File(gitRepositoriesRoot, "/public/my-project"));
        FileUtils.copyDirectory(basedir, new File(gitRepositoriesRoot, "/private/my-project"));

        server = new Server(0);
        ServletHandler servlet = new ServletHandler();

        ServletHolder servletHolder = servlet.addServletWithMapping(GitServlet.class, "/git/*");
        servletHolder.setInitParameter("base-path", gitRepositoriesRoot.getAbsolutePath());
        servletHolder.setInitParameter("export-all", "true");

        servlet.addFilterWithMapping(BasicAuthenticationFilter.class, "/git/private/*", EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(servlet);
        server.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.stop();
        server.join();
    }

    private String publicHttpUrl() {
        return "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort() + "/git/public/my-project";
    }

    private String privateHttpUrl() {
        return "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort() + "/git/private/my-project";
    }

    private String badHttpUrl() {
        return "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort() + "/bad-url";
    }

    @Test
    public void shouldConnectToRemoteHttpRepository() {
        GitConfig config = GitConfig.newBuilder().url(publicHttpUrl()).build();

        GitCommandResult result = GitProcessExecutor.create(GitCommandId.Check_Connection, config)
                                                  .execute();
        assertThat(result.returnValue()).isEqualTo(0);
    }

    @Test
    public void shouldFailOnBadHttpRepositoryUrl() {
        GitConfig config = GitConfig.newBuilder().url(badHttpUrl()).build();

        GitProcessExecutor git = GitProcessExecutor.create(GitCommandId.Check_Connection, config);

        assertThatExceptionOfType(GitCommandExecutionException.class)
            .isThrownBy(git::execute)
            .withMessageContaining("fatal")
            .withMessageContaining("not found");
    }

    @Test
    public void shouldConnectToHttpUrlWithAuthorization() {
        GitConfig config = GitConfig.newBuilder().url(privateHttpUrl())
                                                 .username(BasicAuthenticationFilter.LOGIN_USER)
                                                 .password(BasicAuthenticationFilter.LOGIN_PASSWORD)
                                                 .build();

        GitCommandResult result = GitProcessExecutor.create(GitCommandId.Check_Connection, config)
                .execute();
        assertThat(result.returnValue()).isEqualTo(0);
    }

    @Test
    public void shouldFailWithBadAuthenticationOnHttp() {
        GitConfig config = GitConfig.newBuilder().url(privateHttpUrl())
                                                 .username("bob")
                                                 .password("bad-password")
                                                 .build();

        GitProcessExecutor git = GitProcessExecutor.create(GitCommandId.Check_Connection, config);

        assertThatExceptionOfType(GitCommandExecutionException.class)
            .isThrownBy(git::execute)
            .withMessageContaining("fatal: Authentication failed for 'http://localhost:"
                                    + getServerPort()
                                    + "/git/private/my-project/'");
    }

    @Test
    public void shouldFailWithBadAuthenticationOnHttpWhenCredentialsNotProvided() {
        GitConfig config = GitConfig.newBuilder().url(privateHttpUrl()).build();

        GitProcessExecutor git = GitProcessExecutor.create(GitCommandId.Check_Connection, config);

        assertThatExceptionOfType(GitCommandExecutionException.class)
                .isThrownBy(git::execute)
                .withMessageContaining("fatal: Authentication failed for 'http://localhost:"
                                        + getServerPort()
                                        + "/git/private/my-project/'");
    }

    private int getServerPort(){
        Connector[] connectors = server.getConnectors();
        ServerConnector serverConnector = (ServerConnector)connectors[0];
        return serverConnector.getLocalPort();
    }
}
