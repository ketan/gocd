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

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Shell {
    public static ProcessResult sh(ExecuteStreamHandler streamPumper, File workingDir, Map<String, String> environment, String... args) {
        return sh(streamPumper, workingDir, environment, true, Arrays.asList(args));
    }

    public static ProcessResult sh(ExecuteStreamHandler streamPumper, File workingDir, Map<String, String> environment, Iterable<String> args) {
        return sh(streamPumper, workingDir, environment, true, args);
    }

    public static ProcessResult sh(ExecuteStreamHandler streamPumper, File workingDir, Map<String, String> environment, boolean bufferOutput, String... args) {
        return sh(streamPumper, workingDir, environment, bufferOutput, Arrays.asList(args));
    }

    public static ProcessResult sh(ExecuteStreamHandler streamPumper, File workingDir, Map<String, String> environment, boolean bufferOutput, Iterable<String> args) {
        ProcessExecutor processExecutor = new DefaultProcessExecutor()
                .command(args)
                .directory(workingDir)
                .timeout(600, TimeUnit.SECONDS)
                .readOutput(bufferOutput);

        if (streamPumper != null) {
            processExecutor.streams(streamPumper);
        }

        if (environment != null) {
            processExecutor.environment(environment);
        }

        try {
            return processExecutor.execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
