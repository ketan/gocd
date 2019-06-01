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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CloneCLITest extends AbstractGitTest {

    @Test
    void shouldPerformFullCloneOnDefaultBranch() throws Exception {
        gitRepository.makeCommits(20);

        File targetDir = tempDirectory.resolve("temp-clone").toFile();
        new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir)
                .execute(streamPumper);

        assertThat(getBranch(targetDir)).isEqualTo("master");
        assertThat(gitLogOneLine(targetDir)).isEqualTo(gitLogOneLine(gitRepository.getBasedir()));
    }

    @Test
    void shouldPerformShallowCloneOnDefaultBranch() throws Exception {
        gitRepository.makeCommits(20);

        List<String> expectedCommits = gitLogOneLine(gitRepository.getBasedir());

        File targetDir = tempDirectory.resolve("temp-clone").toFile();
        new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir)
                .depth(2)
                .execute(streamPumper);

        assertThat(getBranch(targetDir)).isEqualTo("master");
        assertThat(gitLogOneLine(targetDir))
                .hasSize(2)
                .containsExactly(expectedCommits.get(0), expectedCommits.get(1));
    }

    @Test
    void shouldBlowUpIfTargetDirectoryAlreadyExists() throws IOException {
        gitRepository.makeCommits(1);

        File targetDir = tempDirectory.resolve("temp-clone").toFile();
        assertThat(targetDir.mkdirs()).isTrue();
        new File(targetDir, UUID.randomUUID().toString()).createNewFile();
        assertThatCode(() -> {
            new CloneCLI()
                    .url(gitRepository.getUrl())
                    .targetDir(targetDir)
                    .execute(streamPumper);
        })
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldCloneSpecifiedBranch() {
        gitRepository.makeCommits(1);
        gitRepository.checkoutAndCreateBranch("some-feature");
        gitRepository.makeCommits(2);

        File targetDir = tempDirectory.resolve("temp-clone").toFile();
        new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir)
                .branch("some-feature")
                .execute(streamPumper);

        assertThat(getBranch(targetDir)).isEqualTo("some-feature");
    }


    @Nested
    class IsShallowClone {
        @Test
        void shouldReturnTrueForShallowCloneIfDepthIsSpecified() {
            assertThat(new CloneCLI().depth(0).isShallowClone()).isFalse();
            assertThat(new CloneCLI().depth(-1).isShallowClone()).isFalse();
            assertThat(new CloneCLI().depth(null).isShallowClone()).isFalse();

            assertThat(new CloneCLI().depth(1).isShallowClone()).isTrue();
        }
    }
}
