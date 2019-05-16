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
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptGeneratorTest {

    @Test
    void generatesUnixScriptWithCredentials() {
        File usernameFile = new File("some\\regular/path/foo");
        File passwordFile = new File("some/regular\\path/bar");
        CommandLine foo = CommandLine.createCommandLine("foo");
        String script = new ScriptGenerator().askpassUnixScript(foo, usernameFile, passwordFile);

        assertThat(foo.env())
                .containsEntry("GO_USERNAME_FILE", usernameFile.getAbsolutePath())
                .containsEntry("GO_PASSWORD_FILE", passwordFile.getAbsolutePath());

        assertThat(script)
                .contains("cat \"${GO_USERNAME_FILE}\"")
                .contains("cat \"${GO_PASSWORD_FILE}\"");
    }

    @Test
    void generatesWindowsScriptWithCredentials() {
        File usernameFile = new File("some\\regular/path/foo");
        File passwordFile = new File("some/regular\\path/bar");
        CommandLine foo = CommandLine.createCommandLine("foo");
        String script = new ScriptGenerator().askpassWindowsScript(foo, usernameFile, passwordFile);

        assertThat(foo.env())
                .containsEntry("GO_USERNAME_FILE", usernameFile.getAbsolutePath())
                .containsEntry("GO_PASSWORD_FILE", passwordFile.getAbsolutePath());

        assertThat(script)
                .contains("type %GO_USERNAME_FILE%")
                .contains("type %GO_PASSWORD_FILE%");
    }

}
