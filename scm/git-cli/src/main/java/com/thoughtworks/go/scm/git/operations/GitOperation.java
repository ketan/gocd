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

import com.thoughtworks.go.scm.git.CLI;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

import java.io.File;
import java.io.IOException;

class GitOperation<T extends CLI> {
    final T cli;
    final ExecuteStreamHandler executeStreamHandler;

    GitOperation(T cli, ExecuteStreamHandler executeStreamHandler) {
        this.cli = cli;
        this.executeStreamHandler = executeStreamHandler;
    }

    protected static void deleteDirectoryNoisily(File defaultDirectory) {
        if (!defaultDirectory.exists()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(defaultDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete directory: " + defaultDirectory.getAbsolutePath(), e);
        }
    }

}
