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

import com.thoughtworks.go.protobufs.pipelineconfig.EnvironmentVariableProto;
import com.thoughtworks.go.protobufs.tasks.ExecProto;
import com.thoughtworks.go.protobufs.work.WorkProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WorkExecutor {
    private final List<Executable> registry = new ArrayList<>();

    public void execute(WorkProto work) {
        work.getTaskList().forEach(exec -> this.execute(exec, work.getEnvironmentVariableList()));
    }

    private void execute(ExecProto exec,
                         List<EnvironmentVariableProto> environmentVariables) {
        final Executable executable = new Executable()
                .environments(environmentVariables)
                .command(exec.getCommand())
                .args(exec.getArgsList())
                .workingDir(exec.getWorkingDir());

        registry.add(executable);
        executable.start();
    }

    public void cancel() {
        registry.forEach(this::cancelRunningProcess);
    }

    private void cancelRunningProcess(Executable executable) {
        if (executable == null) {
            return;
        }

        try {
            executable.cancel();
        } catch (Exception e) {
            log.error("Failed to cancel the task", e);
        }
    }
}
