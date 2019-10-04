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
import com.thoughtworks.go.protobufs.tasks.ExecProto;
import com.thoughtworks.go.protobufs.work.WorkProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.zeroturnaround.exec.stream.NullOutputStream.NULL_OUTPUT_STREAM;

@Component
@Slf4j
public class WorkExecutor {
    public void execute(WorkProto work) {
        work.getTaskList().forEach(this::execute);
    }

    private void execute(ExecProto execProto) {
        log.info("Starting to execute the work.");

        AtomicReference<StartedProcess> processReference = new AtomicReference<>();
        List<String> command = new ArrayList<>(execProto.getArgsList());
        command.add(0, execProto.getCommand());
        //TODO: send it to server
        OutputStream serverOutputStream = new ByteArrayOutputStream();
        try {
            OnIdleOutputStream outputStream = new OnIdleOutputStream(NULL_OUTPUT_STREAM, "idle", 3L, TimeUnit.SECONDS, processRunner(processReference));
            ProcessExecutor processExecutor = new ProcessExecutor().command(command)
                    .redirectOutputAlsoTo(System.out)
                    .redirectErrorAlsoTo(System.err)
                    .redirectOutputAlsoTo(outputStream)
                    .redirectOutputAlsoTo(serverOutputStream)
                    .redirectErrorAlsoTo(outputStream)
                    .redirectErrorAlsoTo(serverOutputStream)
                    .destroyOnExit();

            if (isNotBlank(execProto.getWorkingDir())) {
                processExecutor.directory(new File(execProto.getWorkingDir()));
            }

            processReference.set(processExecutor.start());
        } catch (IOException e) {
            log.error("Task execution failed for the task {}", execProto, e);
        }
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
