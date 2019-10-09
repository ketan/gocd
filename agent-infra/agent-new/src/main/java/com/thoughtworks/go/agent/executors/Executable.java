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

package com.thoughtworks.go.agent.executors;

import com.thoughtworks.go.agent.io.OnIdleOutputStream;
import com.thoughtworks.go.protobufs.pipelineconfig.EnvironmentVariableProto;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.process.PidUtil;
import org.zeroturnaround.process.ProcessUtil;
import org.zeroturnaround.process.Processes;
import org.zeroturnaround.process.SystemProcess;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static org.zeroturnaround.exec.stream.NullOutputStream.NULL_OUTPUT_STREAM;

@Slf4j
@Setter
@Accessors(fluent = true)
public class Executable {
    private String command;
    private String workingDir;
    private List<String> args = new ArrayList<>();
    private List<EnvironmentVariableProto> environments = new ArrayList<>();
    private final OutputStream serverOutputStream = new ByteArrayOutputStream();
    private final AtomicReference<StartedProcess> processReference = new AtomicReference<>();

    public void start() {
        OnIdleOutputStream outputStream = new OnIdleOutputStream(NULL_OUTPUT_STREAM, "idle", 3L, TimeUnit.SECONDS, processRunner(processReference));
        ProcessExecutor processExecutor = new ProcessExecutor()
                .command(getCommand())
                .directory(new File(workingDir))
                .redirectOutputAlsoTo(System.out)
                .redirectErrorAlsoTo(System.err)
                .redirectOutputAlsoTo(serverOutputStream)
                .redirectErrorAlsoTo(serverOutputStream)
                .redirectOutputAlsoTo(outputStream)
                .redirectErrorAlsoTo(outputStream)
                .destroyOnExit();

        try {
            processReference.set(processExecutor.start());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        try {
            log.info(format("Cancelling the process %s", processReference.get()));
            processReference.get().getProcess().destroy();
        } catch (Exception e) {
            log.error("Failed to cancel the process", e);
        }
    }

    private List<String> getCommand() {
        final List<String> list = new ArrayList<>(args);
        list.add(0, command);
        return list;
    }

    private Runnable processRunner(AtomicReference<StartedProcess> processReference) {
        return () -> {
            try {
                log.info("Task execution timed out. Attempting to terminate task.");
                Process process = processReference.get().getProcess();
                SystemProcess newStandardProcess = Processes.newStandardProcess(process, PidUtil.getPid(process));
                ProcessUtil.destroyGracefullyOrForcefullyAndWait(newStandardProcess, 30, TimeUnit.SECONDS, 30, TimeUnit.SECONDS);
                log.info("Task was terminated successfully.");
            } catch (Exception e) {
                log.error("There was an error terminating the task.", e);
            }
        };
    }
}
