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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.config.materials.ScmMaterialConfig.ENCRYPTED_PASSWORD;
import static com.thoughtworks.go.config.materials.ScmMaterialConfig.PASSWORD;
import static com.thoughtworks.go.config.materials.git.GitMaterialConfig.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class PasswordDeserializer {
    public GoCipher goCipher;

    public PasswordDeserializer() {
        this.goCipher = new GoCipher();
    }

    public String deserialize(String password, String encryptedPassword, AbstractMaterialConfig materialConfig) {
        return this.deserialize(password, encryptedPassword, PASSWORD, ENCRYPTED_PASSWORD, materialConfig);
    }

    public String deserializeSshPrivateKey(String sshPrivateKey, String encryptedSshPrivateKey, GitMaterialConfig materialConfig) {
        return this.deserialize(sshPrivateKey, encryptedSshPrivateKey, SSH_PRIVATE_KEY, ENCRYPTED_SSH_PRIVATE_KEY, materialConfig);
    }

    public String deserializeSshPassphrase(String sshPassphrase, String encryptedSshPassphrase, GitMaterialConfig materialConfig) {
        return this.deserialize(sshPassphrase, encryptedSshPassphrase, SSH_PASSPHRASE, ENCRYPTED_SSH_PASSPHRASE, materialConfig);
    }

    public String deserialize(String value, String encryptedValue, String fieldName, String encryptedFieldName, AbstractMaterialConfig materialConfig) {
        if (isNotBlank(value) && isNotBlank(encryptedValue)) {
            String message = String.format("You may only specify `%s` or `encrypted_%s`, not both!", fieldName, fieldName);
            materialConfig.addError(fieldName, message);
            materialConfig.addError(encryptedFieldName, message);
        }

        if (isNotBlank(value)) {
            try {
                return goCipher.encrypt(value);
            } catch (CryptoException e) {
                materialConfig.addError(fieldName, String.format("Could not encrypt the %s. This usually happens when the cipher text is invalid", fieldName));
            }
        } else if (isNotBlank(encryptedValue)) {
            try {
                goCipher.decrypt(encryptedValue);
            } catch (Exception e) {
                materialConfig.addError(encryptedFieldName, String.format("Encrypted value for %s is invalid. This usually happens when the cipher text is invalid.", fieldName));
            }

            return goCipher.maybeReEncryptForPostConstructWithoutExceptions(encryptedValue);
        }
        return null;
    }
}
