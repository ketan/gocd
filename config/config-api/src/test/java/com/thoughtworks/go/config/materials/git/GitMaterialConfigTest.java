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

package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.config.rules.SupportedEntity.PIPELINE_GROUP;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.gitMaterialConfig;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GitMaterialConfigTest {
    @Test
    void shouldBePasswordAwareMaterial() {
        assertThat(git()).isInstanceOf(PasswordAwareMaterial.class);
    }

    @Test
    void shouldSetConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("");

        Map<String, String> map = new HashMap<>();
        map.put(GitMaterialConfig.URL, "url");
        map.put(GitMaterialConfig.BRANCH, "some-branch");
        map.put(GitMaterialConfig.SHALLOW_CLONE, "true");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, null);
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        map.put(GitMaterialConfig.SSH_PRIVATE_KEY, "this_is_the_test_private_key_content");
        map.put(GitMaterialConfig.SSH_PRIVATE_KEY_CHANGED, "1");
        map.put(GitMaterialConfig.SSH_PASSPHRASE, "this_is_test_passphrase");
        map.put(GitMaterialConfig.SSH_PASSPHRASE_CHANGED, "1");

        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("some-branch");
        assertThat(gitMaterialConfig.getName()).isEqualTo(new CaseInsensitiveString("material-name"));
        assertThat(gitMaterialConfig.isAutoUpdate()).isFalse();
        assertThat(gitMaterialConfig.isShallowClone()).isTrue();
        assertThat(gitMaterialConfig.filter()).isEqualTo(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help")));

        assertThat(gitMaterialConfig.getEncryptedSshPrivateKey()).isNotBlank();
        assertThat(gitMaterialConfig.getSshPrivateKey()).isNull();
        assertThat(gitMaterialConfig.getEncryptedSshPassphrase()).isNotBlank();
        assertThat(gitMaterialConfig.getSshPassphrase()).isNull();
    }

    @Test
    void setConfigAttributes_shouldUpdatePasswordOnlyWhenItsChangedFlagIsSet() throws Exception {
        GitMaterialConfig gitMaterialConfig = git("");
        Map<String, String> map = new HashMap<>();
        map.put(ScmMaterialConfig.PASSWORD, "secret");
        map.put(ScmMaterialConfig.PASSWORD_CHANGED, "1");

        gitMaterialConfig.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(gitMaterialConfig, "password")).isNull();
        assertThat(gitMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(gitMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        //Dont change
        map.put(ScmMaterialConfig.PASSWORD, "Hehehe");
        map.put(ScmMaterialConfig.PASSWORD_CHANGED, "0");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(gitMaterialConfig, "password")).isNull();
        assertThat(gitMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(gitMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        map.put(ScmMaterialConfig.PASSWORD, "");
        map.put(ScmMaterialConfig.PASSWORD_CHANGED, "1");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getPassword()).isNull();
        assertThat(gitMaterialConfig.getEncryptedPassword()).isNull();
    }

    @Test
    void setConfigAttributes_shouldUpdatePrivateKeyOnlyWhenItsChangedFlagIsSet() {
        // Should change
        GitMaterialConfig gitMaterialConfig = git("");
        Map<String, String> map = new HashMap<>();
        map.put(GitMaterialConfig.SSH_PRIVATE_KEY, "this_is_my_secret_private_key");
        map.put(GitMaterialConfig.SSH_PRIVATE_KEY_CHANGED, "1");

        gitMaterialConfig.setConfigAttributes(map);
        assertThat(gitMaterialConfig.getSshPrivateKey()).isBlank();
        assertThat(gitMaterialConfig.getEncryptedSshPrivateKey()).isNotBlank();
        assertThat(gitMaterialConfig.currentSshPrivateKey()).isEqualTo("this_is_my_secret_private_key");

        // Should not change
        map.put(GitMaterialConfig.SSH_PRIVATE_KEY, "this_is_my_another_secret_private_key");
        map.put(GitMaterialConfig.SSH_PRIVATE_KEY_CHANGED, "0");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getSshPrivateKey()).isBlank();
        assertThat(gitMaterialConfig.getEncryptedSshPrivateKey()).isNotBlank();
        assertThat(gitMaterialConfig.currentSshPrivateKey()).isEqualTo("this_is_my_secret_private_key");

        map.put(GitMaterialConfig.SSH_PRIVATE_KEY, "");
        map.put(GitMaterialConfig.SSH_PRIVATE_KEY_CHANGED, "1");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getSshPrivateKey()).isNull();
        assertThat(gitMaterialConfig.getEncryptedSshPrivateKey()).isNull();
        assertThat(gitMaterialConfig.currentSshPrivateKey()).isNull();
    }


    @Test
    void setConfigAttributes_shouldUpdatePassphraseOnlyWhenItsChangedFlagIsSet() {
        // Should change
        GitMaterialConfig gitMaterialConfig = git("");
        Map<String, String> map = new HashMap<>();
        map.put(GitMaterialConfig.SSH_PASSPHRASE, "this_is_my_passphrase");
        map.put(GitMaterialConfig.SSH_PASSPHRASE_CHANGED, "1");

        gitMaterialConfig.setConfigAttributes(map);
        assertThat(gitMaterialConfig.getSshPassphrase()).isBlank();
        assertThat(gitMaterialConfig.getEncryptedSshPassphrase()).isNotBlank();
        assertThat(gitMaterialConfig.currentSshPassphrase()).isEqualTo("this_is_my_passphrase");

        // Should not change
        map.put(GitMaterialConfig.SSH_PASSPHRASE, "this_is_my_another_passphrase");
        map.put(GitMaterialConfig.SSH_PASSPHRASE_CHANGED, "0");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getSshPassphrase()).isBlank();
        assertThat(gitMaterialConfig.getEncryptedSshPassphrase()).isNotBlank();
        assertThat(gitMaterialConfig.currentSshPassphrase()).isEqualTo("this_is_my_passphrase");

        map.put(GitMaterialConfig.SSH_PASSPHRASE, "");
        map.put(GitMaterialConfig.SSH_PASSPHRASE_CHANGED, "1");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getSshPassphrase()).isNull();
        assertThat(gitMaterialConfig.getEncryptedSshPassphrase()).isNull();
        assertThat(gitMaterialConfig.currentSshPassphrase()).isNull();
    }

    @Test
    void byDefaultShallowCloneShouldBeOff() {
        assertThat(git("http://url", "foo").isShallowClone()).isFalse();
        assertThat(git("http://url", "foo", false).isShallowClone()).isFalse();
        assertThat(git("http://url", "foo", null).isShallowClone()).isFalse();
        assertThat(git("http://url", "foo", true).isShallowClone()).isTrue();
    }

    @Test
    void shouldReturnIfAttributeMapIsNull() {
        GitMaterialConfig gitMaterialConfig = git("");
        gitMaterialConfig.setConfigAttributes(null);
        assertThat(gitMaterialConfig).isEqualTo(git(""));
    }

    @Test
    void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = git(url);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldReturnNullIfUrlForMaterialNotSpecified() {
        GitMaterialConfig config = git();

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldSetUrlForAMaterial() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = git();

        config.setUrl(url);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldHandleNullWhenSettingUrlForAMaterial() {
        GitMaterialConfig config = git();

        config.setUrl(null);

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldHandleNullUrlAtTheTimeOfGitMaterialConfigCreation() {
        GitMaterialConfig config = git(null);

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldHandleNullBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, null));
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("master");
    }

    @Test
    void shouldHandleEmptyBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, "     "));
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("master");
    }

    @Nested
    class Validate {
        @Test
        void shouldEnsureUrlIsNotBlank() {
            GitMaterialConfig gitMaterialConfig = git("");
            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));
            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isEqualTo("URL cannot be blank");
        }

        @Test
        void shouldEnsureUserNameIsNotProvidedInBothUrlAsWellAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");
            gitMaterialConfig.setUserName("user");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isEqualTo("Ambiguous credentials, must be provided either in URL or as attributes.");
        }

        @Test
        void shouldEnsurePasswordIsNotProvidedInBothUrlAsWellAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");
            gitMaterialConfig.setPassword("pass");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isEqualTo("Ambiguous credentials, must be provided either in URL or as attributes.");
        }

        @Test
        void shouldIgnoreInvalidUrlForCredentialValidation() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com##dobule-hash-is-invalid-in-url");
            gitMaterialConfig.setUserName("user");
            gitMaterialConfig.setPassword("password");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isNull();
        }

        @Test
        void shouldFailWhenBothUsernamePasswordAndSecretKeyPassphraseAreProvided() {
            final GitMaterialConfig gitMaterialConfig = gitMaterialConfig("https://username:{{SECRET:[secret_config_id][pass]}}@host/foo.git");

            Map<String, String> map = new HashMap<>();
            map.put(ScmMaterialConfig.USERNAME, "admin");
            map.put(ScmMaterialConfig.PASSWORD, "secret");
            map.put(ScmMaterialConfig.PASSWORD_CHANGED, "1");

            map.put(GitMaterialConfig.SSH_PRIVATE_KEY, "this_is_my_private_key");
            map.put(GitMaterialConfig.SSH_PRIVATE_KEY_CHANGED, "1");

            gitMaterialConfig.setConfigAttributes(map);
            assertThat(gitMaterialConfig.validateTree(mockValidationContextForSecretParams())).isFalse();
            assertThat(gitMaterialConfig.errors().on("sshPrivateKey"))
                                                 .isEqualTo("Only username/password or private-key/passphrase is allowed");

            map.put(GitMaterialConfig.SSH_PRIVATE_KEY, "");
            map.put(GitMaterialConfig.SSH_PRIVATE_KEY_CHANGED, "1");
            map.put(GitMaterialConfig.SSH_PASSPHRASE, "this_is_my_passphrase");
            map.put(GitMaterialConfig.SSH_PASSPHRASE_CHANGED, "1");
            gitMaterialConfig.setConfigAttributes(map);

            assertThat(gitMaterialConfig.validateTree(mockValidationContextForSecretParams())).isFalse();
            assertThat(gitMaterialConfig.errors().on("sshPassphrase"))
                                                 .isEqualTo("Only username/password or private-key/passphrase is allowed");
        }

        @Test
        void shouldNotFailWhenUsernamePasswordAreSpecifiedAndSecretKeyPassphraseAreNotSpecified() {
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file");
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);

            final GitMaterialConfig gitMaterialConfig = gitMaterialConfig("https://username:{{SECRET:[secret_config_id][pass]}}@host/foo.git");

            Map<String, String> map = new HashMap<>();
            map.put(ScmMaterialConfig.USERNAME, "admin");
            map.put(ScmMaterialConfig.PASSWORD, "secret");
            map.put(ScmMaterialConfig.PASSWORD_CHANGED, "1");

            gitMaterialConfig.setConfigAttributes(map);
            assertThat(gitMaterialConfig.validateTree(validationContext)).isTrue();
        }

        @Test
        void shouldNotFailWhenSecretKetOrPassphraseAreSpecifiedAndUsernamePasswordAreNotSpecified() {
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file");
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            final GitMaterialConfig gitMaterialConfig = gitMaterialConfig("https://username:{{SECRET:[secret_config_id][pass]}}@host/foo.git");

            Map<String, String> map = new HashMap<>();
            map.put(GitMaterialConfig.SSH_PRIVATE_KEY, "this_is_my_private_key");
            map.put(GitMaterialConfig.SSH_PRIVATE_KEY_CHANGED, "1");
            gitMaterialConfig.setConfigAttributes(map);

            assertThat(gitMaterialConfig.validateTree(validationContext)).isTrue();

            map = new HashMap<>();
            map.put(GitMaterialConfig.SSH_PASSPHRASE, "this_is_my_passphrase");
            map.put(GitMaterialConfig.SSH_PASSPHRASE_CHANGED, "1");
            gitMaterialConfig.setConfigAttributes(map);

            assertThat(gitMaterialConfig.validateTree(validationContext)).isTrue();
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyInUrl() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isNull();
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://example.com");
            gitMaterialConfig.setUserName("bob");
            gitMaterialConfig.setPassword("badger");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isNull();
        }
    }

    @Nested
    class ValidateTree {
        @Test
        void shouldCallValidate() {
            final MaterialConfig materialConfig = spy(git("https://example.repo"));
            final ValidationContext validationContext = mockValidationContextForSecretParams();

            materialConfig.validateTree(validationContext);

            verify(materialConfig).validate(validationContext);
        }

        @Test
        void shouldFailIfSecretConfigCannotBeUsedInPipelineGroupWhereCurrentMaterialIsDefined() {
            GitMaterialConfig gitMaterialConfig = git("https://example.repo");
            gitMaterialConfig.setUserName("bob");
            gitMaterialConfig.setPassword("{{SECRET:[secret_config_id][pass]}}");
            final Rules directives = new Rules(new Allow("refer", PIPELINE_GROUP.getType(), "group_2"));
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file", directives);
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            when(validationContext.getPipelineGroup()).thenReturn(createGroup("group_1", "up42"));

            assertThat(gitMaterialConfig.validateTree(validationContext)).isFalse();

            assertThat(gitMaterialConfig.errors().get("encryptedPassword"))
                    .contains("Secret config with ids `secret_config_id` is not allowed to use in `pipelines` with name `group_1`.");
        }

        @Test
        void shouldPassIfSecretConfigCanBeReferredInPipelineGroupWhereCurrentMaterialIsDefined() {
            GitMaterialConfig gitMaterialConfig = git("https://example.repo");
            gitMaterialConfig.setUserName("bob");
            gitMaterialConfig.setPassword("{{SECRET:[secret_config_id][pass]}}");
            final Rules directives = new Rules(
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_2"),
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_1")
            );
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file", directives);
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            when(validationContext.getPipelineGroup()).thenReturn(createGroup("group_1", "up42"));

            assertThat(gitMaterialConfig.validateTree(validationContext)).isTrue();

            assertThat(gitMaterialConfig.errors().getAll()).isEmpty();
        }
    }

    @Nested
    class Equals {
        @Test
        void shouldBeEqualIfObjectsHaveSameUrlBranchAndSubModuleFolder() {
            final GitMaterialConfig material_1 = git("http://example.com", "master");
            material_1.setUserName("bob");
            material_1.setSubmoduleFolder("/var/lib/git");

            final GitMaterialConfig material_2 = git("http://example.com", "master");
            material_2.setUserName("alice");
            material_2.setSubmoduleFolder("/var/lib/git");

            assertThat(material_1.equals(material_2)).isTrue();
        }
    }

    @Nested
    class Fingerprint {
        @Test
        void shouldGenerateFingerprintForGivenMaterialUrlAndBranch() {
            GitMaterialConfig gitMaterialConfig = git("https://bob:pass@github.com/gocd", "feature");

            assertThat(gitMaterialConfig.getFingerprint()).isEqualTo("755da7fb7415c8674bdf5f8a4ba48fc3e071e5de429b1308ccf8949d215bdb08");
        }
    }

    private ValidationContext mockValidationContextForSecretParams(SecretConfig... secretConfigs) {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getSecretConfigs()).thenReturn(new SecretConfigs(secretConfigs));
        return validationContext;
    }
}
