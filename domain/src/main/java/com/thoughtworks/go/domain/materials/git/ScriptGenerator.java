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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class ScriptGenerator {

    String askpassUnixScript(CommandLine commandLine, File usernameFile, File passwordFile) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
                printWriter.println("#!/bin/sh");
//                printWriter.println("echo \"Invoked askpass script with args $@\" >> /tmp/git-ssh-trace.log 2>&1");
                printWriter.println("case \"$1\" in");
                printWriter.println("  Username*) cat \"${GO_USERNAME_FILE}\" ;;");
                printWriter.println("  Password*) cat \"${GO_PASSWORD_FILE}\" ;;");
                printWriter.println("esac");
            }
            commandLine.withEnv("GO_USERNAME_FILE", usernameFile.getAbsolutePath());
            commandLine.withEnv("GO_PASSWORD_FILE", passwordFile.getAbsolutePath());
            return baos.toString("utf-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String askpassWindowsScript(CommandLine commandLine, File usernameFile, File passwordFile) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

            try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
                printWriter.println("@set arg=%~1");
                printWriter.println("@if (%arg:~0,8%)==(Username) type %GO_USERNAME_FILE%");
                printWriter.println("@if (%arg:~0,8%)==(Password) type %GO_PASSWORD_FILE%");
            }

            commandLine.withEnv("GO_USERNAME_FILE", usernameFile.getAbsolutePath());
            commandLine.withEnv("GO_PASSWORD_FILE", passwordFile.getAbsolutePath());
            return baos.toString("utf-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public String sshUnixScript(CommandLine commandLine, File sshPrivateKeyFile) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
                printWriter.println("#!/bin/sh");
                // ${SSH_ASKPASS} might be ignored if ${DISPLAY} is not set
//                printWriter.println("echo \"Invoked ssh script with args $@\" >> /tmp/git-ssh-trace.log 2>&1");
                printWriter.println("if [ -z \"${DISPLAY}\" ]; then");
                printWriter.println("  DISPLAY=:123.456");
                printWriter.println("  export DISPLAY");
                printWriter.println("fi");
                printWriter.println(String.format("ssh -vvv -i \"%s\" -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \"$@\"", sshPrivateKeyFile.getAbsolutePath()));
            }
            commandLine.withEnv("GO_PRIVATE_KEY_FILE", sshPrivateKeyFile.getAbsolutePath());
            return baos.toString("utf-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
