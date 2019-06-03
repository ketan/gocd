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

package com.thoughtworks.go.scm.git.operations;

import com.thoughtworks.go.scm.git.cli.AbstractGitTest;
import com.thoughtworks.go.scm.git.cli.CloneCLI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import static com.thoughtworks.go.scm.git.Shell.sh;
import static org.assertj.core.api.Assertions.assertThat;

class EnsureClonedTest extends AbstractGitTest {

    @BeforeEach
    void setUp() {
        gitRepository.makeCommits(5);
    }

    @Test
    void shouldPerformSimpleCloneIfDirectoryDoesNotExist() throws Exception {
        File targetDir = tempDirectory.resolve("temp-clone").toFile();
        CloneCLI cloneCLI = new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir);

        new EnsureCloned(cloneCLI, streamPumper).execute();

        assertThat(gitLogOneLine(targetDir)).isEqualTo(gitLogOneLine(gitRepository.getBasedir()));
    }

    @Test
    void shouldRemoveTargetDirIfRepoDoesNotLookLikeAGitRepository() throws Exception {
        File targetDir = tempDirectory.resolve("temp-clone").toFile();
        targetDir.mkdir();

        File someTempFileInWhatDoesNotLookLikeAGitRepo = new File(targetDir, UUID.randomUUID().toString());
        assertThat(someTempFileInWhatDoesNotLookLikeAGitRepo.createNewFile()).isTrue();

        CloneCLI cloneCLI = new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir);

        new EnsureCloned(cloneCLI, streamPumper).execute();

        assertThat(gitLogOneLine(targetDir)).isEqualTo(gitLogOneLine(gitRepository.getBasedir()));
    }

    @Test
    void shouldRemoveTargetDirIfRepoDoesPointsToIncorrectBranch_Aka_Branch_Changed() throws Exception {
        File targetDir = tempDirectory.resolve("temp-clone").toFile();

        CloneCLI cloneCLI = new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir);

        cloneCLI.execute(streamPumper);

        // checkout a different branch in targetDir
        sh(streamPumper, targetDir, null, Arrays.asList("git", "checkout", "-b", "some-feature-branch"));

        assertThat(getBranch(targetDir)).isEqualTo("some-feature-branch");

        new EnsureCloned(cloneCLI, streamPumper).execute();
        assertThat(getBranch(targetDir)).isEqualTo("master");
        assertThat(gitLogOneLine(targetDir)).isEqualTo(gitLogOneLine(gitRepository.getBasedir()));
    }

    @Test
    void shouldRemoveTargetDirIfRepoShallowCloneOptionChanged() {
        File targetDir = tempDirectory.resolve("temp-clone").toFile();

        new CloneCLI()
                .url(gitRepository.getUrl())
                .depth(1)
                .targetDir(targetDir).execute(streamPumper);

        assertThat(gitLogOneLine(gitRepository.getBasedir())).isNotEqualTo(gitLogOneLine(targetDir));

        new EnsureCloned(new CloneCLI()
                .url(gitRepository.getUrl())
                .targetDir(targetDir), streamPumper).execute();

        assertThat(gitLogOneLine(gitRepository.getBasedir())).isEqualTo(gitLogOneLine(targetDir));
    }
}
