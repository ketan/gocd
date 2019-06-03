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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.thoughtworks.go.scm.git.CLI;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.scm.git.Shell.sh;

@Accessors(chain = true, fluent = true)
@Getter
@Setter
public class LogCLI implements CLI {
    private File workingDir;
    private final List<String> args = new ArrayList<>();

    public LogCLI() {
    }

    public LogCLI(LogCLI logCLI) {
        this.workingDir = logCLI.workingDir;
        this.args.addAll(logCLI.args);
    }

    @Override
    public ProcessResult execute(ExecuteStreamHandler streamPumper) {
        return sh(streamPumper, workingDir, null, Iterables.concat(defaultArgs(), args));
    }

    private List<String> defaultArgs() {
        return ImmutableList.of("git", "log", "--date=iso", "--no-decorate", "--pretty=medium", "--no-color");
    }

    public LogCLI args(String... args) {
        return args(Arrays.asList(args));
    }

    public LogCLI args(Collection<String> args) {
        this.args.addAll(args);
        return this;
    }


}
