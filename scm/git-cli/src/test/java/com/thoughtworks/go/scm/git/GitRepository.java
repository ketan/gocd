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

package com.thoughtworks.go.scm.git;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;

import java.io.Closeable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class GitRepository implements Closeable {
    @Getter
    private final File basedir;
    private final Git git;

    @SneakyThrows
    public GitRepository(File basedir) {
        this.basedir = basedir;
        this.git = Git.init().setDirectory(basedir).call();
    }

    public String getUrl() {
        return this.basedir.toPath().toUri().toString();
    }

    @Override
    public void close() {
        git.close();
    }

    public void simpleRepo() {
        makeCommit("Initial commit");
        makeCommit("Second commit");

        checkoutAndCreateBranch("feature/foo");
        makeCommit("Implement feature foo");
        makeCommit("Fix a bug with feature foo");

        checkoutBranch("master");

        checkoutAndCreateBranch("feature/bar");
        makeCommit("Implement feature bar");
        makeCommit("Fix a bug with feature bar");
        checkoutBranch("master");
    }

    @SneakyThrows
    public void checkoutBranch(String branchName) {
        git.checkout().setName(branchName).call();
    }

    @SneakyThrows
    public void checkoutAndCreateBranch(String branchName) {
        git.checkout().setCreateBranch(true).setName(branchName).call();
    }

    @SneakyThrows
    public void makeCommit(String message) {
        FileUtils.writeStringToFile(new File(git.getRepository().getDirectory(), "README.MD"), UUID.randomUUID().toString(), StandardCharsets.UTF_8);
        git.add().setUpdate(true).addFilepattern(".").call();
        git.commit().setMessage(message).call();
    }

    public void makeCommits(int number) {
        for (int i = 1; i <= number; i++) {
            makeCommit("Commit " + i + "/" + number);
        }
    }
}
