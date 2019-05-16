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

package com.thoughtworks.go.apiv3.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;

class GitMaterialRepresenter implements MaterialRepresenter<GitMaterialConfig> {

    @Override
    public void toJSON(OutputWriter json, GitMaterialConfig material) {
        json.add("name", material.getName());
        json.add("auto_update", material.getAutoUpdate());
        json.add("url", material.getUrl());
        json.addIfNotNull("username", material.getUserName());
        json.addIfNotNull("encrypted_password", material.getEncryptedPassword());
        json.addWithDefaultIfBlank("branch", material.getBranch(), "master");
        json.addIfNotNull("encrypted_ssh_private_key", material.getEncryptedSshPrivateKey());
        json.addIfNotNull("encrypted_ssh_passphrase", material.getEncryptedSshPassphrase());
    }

    @Override
    public GitMaterialConfig fromJSON(JsonReader json) {
        GitMaterialConfig materialConfig = new GitMaterialConfig();
        json.readStringIfPresent("name", materialConfig::setName);
        json.readBooleanIfPresent("auto_update", materialConfig::setAutoUpdate);
        json.readStringIfPresent("branch", materialConfig::setBranch);
        json.readStringIfPresent("username", materialConfig::setUserName);
        json.readStringIfPresent("url", materialConfig::setUrl);

        String password = json.getStringOrDefault("password", null);
        String encryptedPassword = json.getStringOrDefault("encrypted_password", null);
        materialConfig.setEncryptedPassword(PASSWORD_DESERIALIZER.deserialize(password, encryptedPassword, materialConfig));

        String sshPrivateKey = json.getStringOrDefault("ssh_private_key", null);
        String encryptedSshPrivateKey = json.getStringOrDefault("encrypted_ssh_private_key", null);
        materialConfig.setEncryptedSshPrivateKey(PASSWORD_DESERIALIZER.deserializeSshPrivateKey(sshPrivateKey, encryptedSshPrivateKey, materialConfig));

        String sshPassphrase = json.getStringOrDefault("ssh_passphrase", null);
        String encryptedSshPassphrase = json.getStringOrDefault("encrypted_ssh_passphrase", null);
        materialConfig.setEncryptedSshPassphrase(PASSWORD_DESERIALIZER.deserializeSshPassphrase(sshPassphrase, encryptedSshPassphrase, materialConfig));

        return materialConfig;
    }
}
