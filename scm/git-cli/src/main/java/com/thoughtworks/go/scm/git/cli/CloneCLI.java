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

import com.google.common.collect.Lists;
import com.thoughtworks.go.scm.git.CLI;
import com.thoughtworks.go.scm.git.Credentials;
import com.thoughtworks.go.scm.git.Shell;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

import java.io.File;
import java.util.List;

import static com.thoughtworks.go.scm.git.UrlUtil.*;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@Accessors(chain = true, fluent = true)
@Getter
@Setter
public class CloneCLI implements CLI {
    private String url;
    private File targetDir;
    private String branch;
    private Integer depth;
    private Credentials credentials;

    @Override
    public ProcessResult execute(ExecuteStreamHandler streamPumper) {
        ProcessResult processResult = null;
        String urlWithoutCredentials = urlWithoutCredentials(url);
        String username = extractUsername(url, credentials);
        String password = extractPassword(url, credentials);

        String branchOption = "--branch=" + defaultIfBlank(branch, "master");

        List<String> args = Lists.newArrayList("git", "clone", branchOption, url, targetDir.getAbsolutePath());

        if (isShallowClone()) {
            args.add("--depth=" + depth.toString());
        }

        return Shell.sh(streamPumper, null, null, args);
    }

    public boolean isShallowClone() {
        return depth != null && depth >= 1;
    }

}


