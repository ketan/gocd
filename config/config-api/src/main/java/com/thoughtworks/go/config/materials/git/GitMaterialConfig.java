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

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ConfigTag("git")
public class GitMaterialConfig extends ScmMaterialConfig implements PasswordAwareMaterial {
    @ConfigAttribute(value = "url")
    private UrlArgument url;

    @ConfigAttribute(value = "branch")
    private String branch = DEFAULT_BRANCH;

    @ConfigAttribute(value = "shallowClone")
    private boolean shallowClone;

    @SkipParameterResolution
    @ConfigAttribute(value = "sshPrivateKey", allowNull = true)
    private String sshPrivateKey;

    @ConfigAttribute(value = "encryptedSshPrivateKey", allowNull = true)
    private String encryptedSshPrivateKey;

    @SkipParameterResolution
    @ConfigAttribute(value = "sshPassphrase", allowNull = true)
    private String sshPassphrase;

    @ConfigAttribute(value = "encryptedSshPassphrase", allowNull = true)
    private String encryptedSshPassphrase;

    private String submoduleFolder;

    public static final String TYPE = "GitMaterial";
    public static final String URL = "url";
    public static final String BRANCH = "branch";
    public static final String DEFAULT_BRANCH = "master";
    public static final String SHALLOW_CLONE = "shallowClone";

    public static final String SSH_PRIVATE_KEY = "sshPrivateKey";
    public static final String ENCRYPTED_SSH_PRIVATE_KEY = "encryptedSshPrivateKey";
    public static final String SSH_PRIVATE_KEY_CHANGED = "sshPrivateKeyChanged";

    public static final String SSH_PASSPHRASE = "sshPassphrase";
    public static final String ENCRYPTED_SSH_PASSPHRASE = "encryptedSshPassphrase";
    public static final String SSH_PASSPHRASE_CHANGED = "sshPassphraseChanged";

    public String getSshPrivateKey(){
        return sshPrivateKey;
    }

    public String getSshPassphrase(){
        return sshPassphrase;
    }

    public GitMaterialConfig() {
        super(TYPE);
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.originalArgument());
        parameters.put("branch", branch);
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("url", url);
        parameters.put("branch", branch);
        parameters.put("shallowClone", shallowClone);
    }

    @Override
    public String getUrl() {
        return url != null ? url.originalArgument() : null;
    }

    @Override
    public void setUrl(String url) {
        if (url != null) {
            this.url = new UrlArgument(url);
        }
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s, Branch: %s", url.forDisplay(), branch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        GitMaterialConfig that = (GitMaterialConfig) o;

        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }
        if (submoduleFolder != null ? !submoduleFolder.equals(that.submoduleFolder) : that.submoduleFolder != null) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return super.equals(that);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (submoduleFolder != null ? submoduleFolder.hashCode() : 0);
        return result;
    }

    @Override
    public void validateConcreteScmMaterial(ValidationContext validationContext) {
        validateMaterialUrl(this.url, validationContext);
        validateCredentials();
        validateSecretParams(validationContext);
        validatePresenceOfAuthDetails();
    }

    private void validatePresenceOfAuthDetails(){
        if(usernameOrPasswordPresent()){
            if(privateKeyPresent()){
                errors().add(SSH_PRIVATE_KEY, "Only username/password or private-key/passphrase is allowed");
            } else if(passphrasePresent()) {
                errors().add(SSH_PASSPHRASE, "Only username/password or private-key/passphrase is allowed");
            }
        }
    }

    private boolean passphrasePresent() {
        return isNotBlank(this.sshPassphrase) || isNotBlank(this.encryptedSshPassphrase);
    }

    private boolean privateKeyPresent() {
        return isNotBlank(this.sshPrivateKey) || isNotBlank(this.encryptedSshPrivateKey);
    }

    private boolean usernameOrPasswordPresent() {
        return isNotBlank(this.userName) || isNotBlank(this.password) || isNotBlank(this.encryptedPassword);
    }

    @Override
    protected String getLocation() {
        return url.forDisplay();
    }

    @Override
    public String getUriForDisplay() {
        return this.url.forDisplay();
    }

    @Override
    public String getTypeForDisplay() {
        return "Git";
    }

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getSubmoduleFolder() {
        return submoduleFolder;
    }

    public void setSubmoduleFolder(String submoduleFolder) {
        this.submoduleFolder = submoduleFolder;
    }

    public void setSshPrivateKey(String sshPrivateKey) {
        resetSshPrivateKey(sshPrivateKey);
    }

    public void setSshPassphrase(String sshPassphrase) {
        resetSshPassphrase(sshPassphrase);
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getShortRevision(String revision) {
        if (revision == null) return null;
        if (revision.length() < 7) return revision;
        return revision.substring(0, 7);
    }

    @Override
    public String toString() {
        return "GitMaterialConfig{" +
                "url=" + url +
                ", branch='" + branch + '\'' +
                ", submoduleFolder='" + submoduleFolder + '\'' +
                ", shallowClone=" + shallowClone +
                '}';
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        super.setConfigAttributes(attributes);

        Map map = (Map) attributes;
        if (map.containsKey(BRANCH)) {
            String branchName = (String) map.get(BRANCH);
            this.branch = StringUtils.isBlank(branchName) ? DEFAULT_BRANCH : branchName;
        }

        if (map.containsKey("userName")) {
            this.userName = (String) map.get("userName");
        }

        if (map.containsKey(PASSWORD_CHANGED) && "1".equals(map.get(PASSWORD_CHANGED))) {
            String passwordToSet = (String) map.get(PASSWORD);
            resetPassword(passwordToSet);
        }

        if (map.containsKey(URL)) {
            this.url = new UrlArgument((String) map.get(URL));
        }

        if (map.containsKey(SSH_PASSPHRASE_CHANGED) && "1".equals(map.get(SSH_PASSPHRASE_CHANGED))) {
            String passphraseToSet = (String) map.get(SSH_PASSPHRASE);
            resetSshPassphrase(passphraseToSet);
        }

        if (map.containsKey(SSH_PRIVATE_KEY_CHANGED) && "1".equals(map.get(SSH_PRIVATE_KEY_CHANGED))) {
            String privateKetToSet = (String) map.get(SSH_PRIVATE_KEY);
            resetSshPrivateKey(privateKetToSet);
        }

        this.shallowClone = "true".equals(map.get(SHALLOW_CLONE));
    }

    private void resetSshPassphrase(String passphrase) {
        if (StringUtils.isBlank(passphrase)) {
            encryptedSshPassphrase = null;
        }

        setPassphraseIfNotBlank(passphrase);
    }

    private void resetSshPrivateKey(String privateKey) {
        if (StringUtils.isBlank(privateKey)) {
            encryptedSshPrivateKey = null;
        }

        setPrivateKeyIfNotBlank(privateKey);
    }

    @PostConstruct
    @Override
    public void ensureEncrypted() {
        super.ensureEncrypted();

        setPassphraseIfNotBlank(sshPassphrase);
        if (encryptedSshPassphrase != null) {
            setEncryptedSshPassphrase(goCipher.maybeReEncryptForPostConstructWithoutExceptions(encryptedSshPassphrase));
        }

        setPrivateKeyIfNotBlank(sshPrivateKey);
        if (encryptedSshPrivateKey != null) {
            setEncryptedSshPrivateKey(goCipher.maybeReEncryptForPostConstructWithoutExceptions(encryptedSshPrivateKey));
        }
    }

    private void setPassphraseIfNotBlank(String passphrase) {
        this.sshPassphrase = StringUtils.stripToNull(passphrase);
        this.encryptedSshPassphrase = StringUtils.stripToNull(encryptedSshPassphrase);

        if (this.sshPassphrase == null) {
            return;
        }
        try {
            this.encryptedSshPassphrase = this.goCipher.encrypt(passphrase);
        } catch (Exception e) {
            bomb("Passphrase encryption failed. Please verify your cipher key.", e);
        }
        this.sshPassphrase = null;
    }

    private void setPrivateKeyIfNotBlank(String privateKey) {
        this.sshPrivateKey = StringUtils.stripToNull(privateKey);
        this.encryptedSshPrivateKey = StringUtils.stripToNull(encryptedSshPrivateKey);

        if (this.sshPrivateKey == null) {
            return;
        }
        try {
            this.encryptedSshPrivateKey = this.goCipher.encrypt(sshPrivateKey);
        } catch (Exception e) {
            bomb("Private Key encryption failed. Please verify your cipher key.", e);
        }
        this.sshPrivateKey = null;
    }

    public boolean isShallowClone() {
        return shallowClone;
    }

    public void setShallowClone(Boolean shallowClone) {
        if (shallowClone != null) {
            this.shallowClone = shallowClone;
        }
    }

    public void setEncryptedSshPassphrase(String encryptedSshPassphrase) {
        this.encryptedSshPassphrase = encryptedSshPassphrase;
    }

    public void setEncryptedSshPrivateKey(String encryptedSshPrivateKey) {
        this.encryptedSshPrivateKey = encryptedSshPrivateKey;
    }

    public String getEncryptedSshPrivateKey(){
        return this.encryptedSshPrivateKey;
    }

    public String getEncryptedSshPassphrase(){
        return this.encryptedSshPassphrase;
    }

    public final String currentSshPrivateKey() {
        try {
            return StringUtils.isBlank(encryptedSshPrivateKey) ? null : this.goCipher.decrypt(encryptedSshPrivateKey);
        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt the private key to get the real private key", e);
        }
    }

    public final String currentSshPassphrase() {
        try {
            return StringUtils.isBlank(encryptedSshPassphrase) ? null : this.goCipher.decrypt(encryptedSshPassphrase);
        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt the passphrase to get the real passphrase", e);
        }
    }
}
