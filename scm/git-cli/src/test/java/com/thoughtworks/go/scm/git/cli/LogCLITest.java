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

package com.thoughtworks.go.scm.git.cli;

import org.assertj.core.api.StringAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogCLITest extends AbstractGitTest {
    @BeforeEach
    void setUp() {
        gitRepository.makeCommits(5);
    }

    @Test
    void shouldExecuteGitLog() {
        File targetDir = tempDirectory.resolve("temp-clone").toFile();
        new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir)
                .execute(streamPumper);

        ProcessResult execute = new LogCLI()
                .workingDir(targetDir)
                .args("--oneline", "--reverse")
                .execute(streamPumper);

        List<String> outputLines = execute
                .getOutput()
                .getLinesAsUTF8();

        assertThat(outputLines)
                .hasSize(5);

        for (int i = 0; i < 5; i++) {
            assertThat(outputLines, StringAssert.class)
                    .element(i).contains("Commit " + (i + 1) + "/5");
        }
    }
}
