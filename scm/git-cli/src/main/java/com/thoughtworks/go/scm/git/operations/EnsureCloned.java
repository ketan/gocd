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

import com.thoughtworks.go.scm.git.UrlUtil;
import com.thoughtworks.go.scm.git.cli.CloneCLI;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

import java.io.File;
import java.util.Arrays;

import static com.thoughtworks.go.scm.git.Shell.sh;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

class EnsureCloned extends GitOperation<CloneCLI> {

    EnsureCloned(CloneCLI cli, ExecuteStreamHandler executeStreamHandler) {
        super(cli, executeStreamHandler);
    }

    void execute() {
        try {
            if (!isGitRepo(cli.targetDir()) || isRepoChanged()) {
                deleteDirectoryNoisily(cli.targetDir());
            }
        } catch (Exception e) {
            // probably a corrupt git repo?
            deleteDirectoryNoisily(cli.targetDir());
        }

        cli.execute(executeStreamHandler);
    }

    private boolean isRepoChanged() {
        return isRepositoryURLChanged() || isRepoBranchChanged() || isShallowCloneOptionChanged();
    }

    private boolean isShallowCloneOptionChanged() {
        boolean currentRepoIsShallowCloned = new File(cli.targetDir(), ".git/shallow").exists();
        return currentRepoIsShallowCloned != cli.isShallowClone();
    }

    private boolean isRepoBranchChanged() {
        String expectedBranch = defaultIfBlank(cli.branch(), "master");

        ProcessResult processResult = sh(null, cli.targetDir(), null, Arrays.asList("git", "rev-parse", "--abbrev-ref", "HEAD"));
        String currentBranch = processResult.getOutput().getLinesAsUTF8().get(0);

        return !expectedBranch.equals(currentBranch);
    }

    private boolean isRepositoryURLChanged() {
        ProcessResult processResult = sh(null, cli.targetDir(), null, Arrays.asList("git", "config", "remote.origin.url"));
        String currentRepoUrl = processResult.getOutput().getLinesAsUTF8().get(0);

        return !UrlUtil.urlWithoutCredentials(cli.url()).equals(currentRepoUrl);
    }

    private boolean isGitRepo(File targetDir) {
        return new File(targetDir, ".git").isDirectory();
    }
}
