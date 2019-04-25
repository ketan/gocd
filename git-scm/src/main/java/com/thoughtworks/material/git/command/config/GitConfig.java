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

package com.thoughtworks.material.git.command.config;

import java.io.File;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GitConfig {
    private String workingDir;
    private String url;
    private String username;
    private String password;
    private String sshKey;
    private String sshKeyPassphrase;
    private String branch;

    private boolean enableTracing;

    private GitConfig(Builder builder) {
        this.workingDir = builder.workingDir;
        this.branch = builder.branch;
        this.url = builder.url;

        this.username = builder.username;
        this.password = builder.password;

        this.sshKey = builder.sshKey;
        this.sshKeyPassphrase = builder.sshKeyPassphrase;
        this.enableTracing = builder.enableTracing;
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public String url() {
        return url;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String sshKey() {
        return sshKey;
    }

    public boolean hasPassword() {
        return isNotBlank(password);
    }

    public boolean hasSshKey() {
        return isNotBlank(sshKey());
    }

    public boolean enableTracing() {
        return false;
    }

    public String sshKeyPassphrase() {
        return sshKeyPassphrase;
    }

    public boolean hasSshKeyPassphrase() {
        return isNotBlank(sshKeyPassphrase);
    }

    public File getWorkingDir() {
        return new File(workingDir);
    }

    public String branch(){
        return branch;
    }

    public static final class Builder {
        private String workingDir;
        private String url;
        private String branch;

        private String username;
        private String password;

        private String sshKey;
        private String sshKeyPassphrase;

        private boolean enableTracing = false;

        private Builder() {
            workingDir(null);
        }

        public GitConfig build(){
            return new GitConfig(this);
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder userName(String username) {
            this.username = username;
            return this;
        }

        public Builder sshKey(String sshKey) {
            this.sshKey = sshKey;
            return this;
        }

        public Builder sshKeyPassphrase(String sshKeyPassphrase) {
            this.sshKeyPassphrase = sshKeyPassphrase;
            return this;
        }

        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            if(this.workingDir == null){
                this.workingDir = ".";
            }

            return this;
        }

        public Builder username(String username){
            this.username = username;
            return this;
        }

        public Builder password(String password){
            this.password = password;
            return this;
        }

        public void enableTracing(boolean enableTracing) {
            this.enableTracing = enableTracing;
        }
    }

}
