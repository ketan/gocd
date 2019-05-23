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

import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@EnableRuleMigrationSupport
public class GitCommandOverSshTest {
    private static final String GIT_REMOTE_PATH = "git/my-project";
    private static final String LOGIN_PASSWORD = "p@ssw0rd";
    private static final String LOGIN_USER = "git";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final String sshKeyPassphrase = UUID.randomUUID().toString();
    private SshServer sshd;
    private File hostKeyFile;
    private KeyPair keyPair1;
    private KeyPair keyPair2;
    private String sshKeyWithPassphrase;
    private File gitRepositoriesRoot;

    @BeforeEach
    void setUp() throws Exception {
        File basedir = temporaryFolder.newFolder("source-root");
        new GitRepository(basedir).initialize();

        gitRepositoriesRoot = temporaryFolder.newFolder("git-root");
        FileUtils.copyDirectory(basedir, new File(gitRepositoriesRoot, GIT_REMOTE_PATH));
        hostKeyFile = temporaryFolder.newFile();

        keyPair1 = keyPairWithoutPassphrase();
        keyPair2 = keyPairWithoutPassphrase();
        sshKeyWithPassphrase = encrypt(keyPair2.getPrivate(), sshKeyPassphrase);

        sshd = SshServer.setUpDefaultServer();
        sshd.setKeyPairProvider(hostKeyProvider(hostKeyFile));
        sshd.setPasswordAuthenticator((username, password, session) -> LOGIN_USER.equals(username) && LOGIN_PASSWORD.equals(password));
        sshd.setPublickeyAuthenticator(new UserPublickeyAuthenticator(keyPair1.getPublic(), keyPair2.getPublic()));
        sshd.setCommandFactory(command -> new ProcessShellFactory(CommandLine.translateCommandLine(command)).create());
        sshd.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sshd != null) {
            sshd.stop(true);
        }
    }

    private static String encrypt(PrivateKey privateKey, String passphrase) throws IOException {
        JcePEMEncryptorBuilder encryptorBuilder = new JcePEMEncryptorBuilder("AES-128-CBC");
        PEMEncryptor encryptor = encryptorBuilder.build(passphrase.toCharArray());
        JcaMiscPEMGenerator pemGenerator = new JcaMiscPEMGenerator(privateKey, encryptor);
        return toPem(pemGenerator.generate());
    }

    private static String toPem(Object pem) throws IOException {
        StringWriter out = new StringWriter();
        JcaPEMWriter writer = new JcaPEMWriter(out);
        writer.writeObject(pem);
        writer.close();
        return out.toString();
    }

    private static KeyPair keyPairWithoutPassphrase() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = SecurityUtils.getKeyPairGenerator("RSA");
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();
    }

    private static AbstractGeneratorHostKeyProvider hostKeyProvider(File hostKeyFile) {
        AbstractGeneratorHostKeyProvider generatorHostKeyProvider = SecurityUtils.createGeneratorHostKeyProvider(hostKeyFile.toPath());
        generatorHostKeyProvider.setAlgorithm("RSA");
        generatorHostKeyProvider.setKeySize(1024);
        generatorHostKeyProvider.loadKeys(null);
        return generatorHostKeyProvider;
    }

    String sshUrl() {
        return String.format("ssh://%s@localhost:%d/%s", LOGIN_USER, sshd.getPort(), new File(gitRepositoriesRoot, GIT_REMOTE_PATH).getAbsolutePath());
    }

    private String badSshUrl() {
        return String.format("ssh://%s@localhost:%d%s", LOGIN_USER, sshd.getPort(), "/git/bad-url");
    }

    @Test
    void shouldAllowSshConnectionUsingSSHKeyWithNoPassphrase() throws Exception {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null, toPem(keyPair1.getPrivate()), null);

        assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(sshUrl()), "master");
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowSshConnectionUsingSSHPassword() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null, null, null);

        assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(sshUrl()), "master", LOGIN_USER, LOGIN_PASSWORD);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldFailIfSshConnectionFailsBecauseOfMissingSshKey() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null, null, null);

        assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(sshUrl()), "master");
        })
                .hasMessageContaining("fatal")
                .hasMessageContaining("/git/my-project")
                .hasMessageContaining("Please make sure you have the correct access rights");
    }

    @Test
    void shouldFailIfSshConnectionFailsBecauseOfBadRepositoryPathInUrl() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null, null, null);

        assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(badSshUrl()), "master", LOGIN_USER, LOGIN_PASSWORD);
        })
                .hasMessageContaining("fatal")
                .hasMessageContaining("git/bad-url")
                .hasMessageContaining("Please make sure you have the correct access rights");
    }

    @Test
    void shouldAllowSshConnectionForSshKeyWithPassphrase() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null, sshKeyWithPassphrase, sshKeyPassphrase);
        assertThatCode(() -> {
            gitCommand.checkConnection(new UrlArgument(sshUrl()), "master");
        })
                .doesNotThrowAnyException();
    }

    private static class UserPublickeyAuthenticator extends KeySetPublickeyAuthenticator {
        UserPublickeyAuthenticator(PublicKey... keys) {
            super(null, Arrays.asList(keys));
        }

        @Override
        public boolean authenticate(String username, PublicKey key, ServerSession session, Collection<? extends PublicKey> keys) {
            return LOGIN_USER.equals(username) && super.authenticate(username, key, session, keys);
        }
    }
}
