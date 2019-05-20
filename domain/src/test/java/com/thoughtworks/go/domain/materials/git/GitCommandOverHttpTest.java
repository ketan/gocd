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

package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
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

import static com.thoughtworks.go.util.UrlUtil.urlWithCredentials;

@EnableRuleMigrationSupport
class GitCommandOverHttpTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File gitRepositoriesRoot;
    private Server server;

    @BeforeEach
    void setUp() throws Exception {
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
    void tearDown() throws Exception {
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
    void shouldConnectToRemoteHttpRepository() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);

        Assertions.assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(publicHttpUrl()), "master");
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldFailOnBadHttpRepositoryUrl() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);

        Assertions.assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(badHttpUrl()), "master");
        })
                .hasMessageContaining("fatal")
                .hasMessageContaining("not found");
    }

    @Test
    void shouldConnectToHttpUrlWithAuthorization() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);

        Assertions.assertThatCode(() -> {
            String urlWithCredentials = urlWithCredentials(privateHttpUrl(), BasicAuthenticationFilter.LOGIN_USER, BasicAuthenticationFilter.LOGIN_PASSWORD);
            gitCommand.checkConnection(new UrlArgument(urlWithCredentials), "master");
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWithBadAuthenticationOnHttp() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);

        Assertions.assertThatCode(() -> {
            String urlWithCredentials = urlWithCredentials(privateHttpUrl(), "bad", "hacker");
            gitCommand.checkConnection(new UrlArgument(urlWithCredentials), "master");
        })
                .hasMessageContaining("fatal")
                .hasMessageContaining("Authentication failed for 'http://localhost:" + getServerPort() + "/git/private/my-project/'");
    }

    @Test
    void shouldFailWithBadAuthenticationOnHttpWhenCredentialsNotProvided() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);

        Assertions.assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(privateHttpUrl()), "master");
        })
                .hasMessageContaining("fatal")
                .hasMessageContaining("Authentication failed for 'http://localhost:" + getServerPort() + "/git/private/my-project/'");
    }

    private int getServerPort() {
        Connector[] connectors = server.getConnectors();
        ServerConnector serverConnector = (ServerConnector) connectors[0];
        return serverConnector.getLocalPort();
    }
}
