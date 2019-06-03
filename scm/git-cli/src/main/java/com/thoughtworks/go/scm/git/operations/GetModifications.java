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

import com.google.common.io.FileBackedOutputStream;
import com.thoughtworks.go.scm.git.GitModificationParser;
import com.thoughtworks.go.scm.git.cli.LogCLI;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;
import org.zeroturnaround.exec.stream.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GetModifications extends GitOperation<LogCLI> {

    GetModifications(LogCLI cli, ExecuteStreamHandler executeStreamHandler) {
        super(cli, executeStreamHandler);
    }

    public void latestModification() {
        try {
            try (FileBackedOutputStream stdout = new FileBackedOutputStream(32 * 1024)) {
                new LogCLI(cli).args("-1").execute(new PumpStreamHandler(stdout, new ByteArrayOutputStream()));
                stdout.close();

                GitModificationParser gitModificationParser = new GitModificationParser();
                stdout.asByteSource().asCharSource(StandardCharsets.UTF_8).readLines(gitModificationParser);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void modificationsSince(String revision) {

    }
}
