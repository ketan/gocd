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

import com.thoughtworks.go.scm.git.cli.CloneCLI;
import com.thoughtworks.go.scm.git.cli.LogCLI;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

public class GitOperations {
    private final ExecuteStreamHandler executeStreamHandler;

    public GitOperations(ExecuteStreamHandler executeStreamHandler) {
        this.executeStreamHandler = executeStreamHandler;
    }

    public void ensureCloned(CloneCLI cli) {
        new EnsureCloned(cli, executeStreamHandler).execute();
    }

    public void latestModification(LogCLI logCLI) {
        new GetModifications(logCLI, executeStreamHandler).latestModification();
    }

    public void modificationsSince(LogCLI logCLI, String revision) {
        logCLI = new LogCLI(logCLI).args("-1");

        new GetModifications(logCLI, executeStreamHandler).modificationsSince(revision);
    }
}
