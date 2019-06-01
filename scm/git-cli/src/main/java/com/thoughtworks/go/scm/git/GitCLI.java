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

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

import java.io.File;

public class GitCLI {

    private static final File TEMP_DIR = new File("data/git-cli");

    static {
        FileUtils.deleteQuietly(TEMP_DIR);
        // cleanup temp dir on exit, if there are any files left behind
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(TEMP_DIR)));
    }

    private final ExecuteStreamHandler executeStreamHandler;

    public GitCLI(ExecuteStreamHandler executeStreamHandler) {
        this.executeStreamHandler = executeStreamHandler;
    }



}
