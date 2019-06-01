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

import com.thoughtworks.go.scm.git.DefaultProcessExecutor;
import com.thoughtworks.go.scm.git.GitRepository;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.exec.stream.PumpStreamHandler;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class AbstractGitTest {
    protected GitRepository gitRepository;
    protected Path tempDirectory;
    protected PumpStreamHandler streamPumper;

    @BeforeEach
    void setUp(@TempDir File upstreamRepository, @TempDir Path tempDirectory) {
        this.gitRepository = new GitRepository(upstreamRepository);
        this.tempDirectory = tempDirectory;
        this.streamPumper = new PumpStreamHandler(Slf4jStream.of("stdout").asInfo(), Slf4jStream.of("stderr").asInfo());
    }

    @AfterEach
    void tearDown() {
        if (gitRepository != null) {
            gitRepository.close();
        }
    }

    @SneakyThrows
    protected List<String> gitLogOneLine(File basedir) {
        return new DefaultProcessExecutor().directory(basedir).command("git", "log", "--oneline").readOutput(true).execute().getOutput().getLinesAsUTF8();
    }

    @SneakyThrows
    protected String getBranch(File basedir) {
        try (Git git = Git.open(basedir)) {
            return git.getRepository().getBranch();
        }
    }
}
