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

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface GitCommand {
    int cloneWithNoCheckout(ConsoleOutputStreamConsumer outputStreamConsumer, String url);

    int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url);

    // Clone repository from url with specified depth.
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as full clone
    int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url, Integer depth);

    List<Modification> latestModification();

    List<Modification> modificationsSince(Revision revision);

    void resetWorkingDir(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision, boolean shallow);

    void resetHard(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision);

    void fetchAndResetToHead(ConsoleOutputStreamConsumer outputStreamConsumer);

    void fetchAndResetToHead(ConsoleOutputStreamConsumer outputStreamConsumer, boolean shallow);

    void updateSubmoduleWithInit(ConsoleOutputStreamConsumer outputStreamConsumer, boolean shallow);

    UrlArgument workingRepositoryUrl();

    void checkConnection(UrlArgument repoUrl, String branch, Map<String, String> environment);

    GitVersion version(Map<String, String> map);

    void add(File fileToAdd);

    void commit(String message);

    void push();

    void pull();

    void fetch(ConsoleOutputStreamConsumer outputStreamConsumer);

    // Unshallow a shallow cloned repository with "git fetch --depth n".
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as infinite -- fully unshallow
    // https://git-scm.com/docs/git-fetch-pack
    void unshallow(ConsoleOutputStreamConsumer outputStreamConsumer, Integer depth);

    void init();

    void submoduleAdd(String repoUrl, String submoduleName, String folder);

    void submoduleRemove(String folderName);

    String currentRevision();

    Map<String, String> submoduleUrls();

    String getCurrentBranch();

    void changeSubmoduleUrl(String submoduleName, String newUrl);

    void submoduleSync();

    boolean isShallow();

    boolean containsRevisionInBranch(Revision revision);

    @Deprecated //tests only
    void checkoutRemoteBranchToLocal();
}
