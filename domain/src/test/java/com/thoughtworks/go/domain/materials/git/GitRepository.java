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

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

class GitRepository {
    private final File basedir;

    GitRepository(File basedir) {
        this.basedir = basedir;
    }

    void initialize() throws GitAPIException, IOException {
        this.basedir.mkdirs();

        Git git = Git.init().setDirectory(basedir).call();

        makeCommit(git, "Initial commit");
        makeCommit(git, "Second commit");

        makeBranch(git, "feature/foo");
        makeCommit(git, "Implement feature foo");
        makeCommit(git, "Fix a bug with feature foo");

        checkoutBranch(git, "master");

        makeBranch(git, "feature/bar");
        makeCommit(git, "Implement feature bar");
        makeCommit(git, "Fix a bug with feature bar");
    }

    private void checkoutBranch(Git git, String branchName) throws GitAPIException {
        git.checkout().setName(branchName).call();
    }

    private void makeBranch(Git git, String branchName) throws GitAPIException {
        git.checkout().setCreateBranch(true).setName(branchName).call();
    }

    private void makeCommit(Git git, String message) throws IOException, GitAPIException {
        FileUtils.writeStringToFile(new File(git.getRepository().getDirectory(), "README.MD"), UUID.randomUUID().toString(), StandardCharsets.UTF_8);
        git.add().setUpdate(true).addFilepattern(".").call();
        git.commit().setMessage(message).call();
    }
}
