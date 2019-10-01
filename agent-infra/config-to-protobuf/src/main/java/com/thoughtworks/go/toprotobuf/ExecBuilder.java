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

package com.thoughtworks.go.toprotobuf;

import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.protobufs.tasks.ProtoExec;
import org.apache.tools.ant.types.Commandline;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

class ExecBuilder implements Builder<ExecTask, ProtoExec> {
    @Override
    public ProtoExec build(ExecTask task) {
        ProtoExec.Builder builder = ProtoExec.newBuilder()
                .setCommand(task.getCommand())
                .addAllArgs(List.of(task.getArgList().toStringArray()));

        if (isNotBlank(task.workingDirectory())) {
            builder.setWorkingDir(task.workingDirectory());
        }

        if (isNotBlank(task.getArgs())) {
            builder.addAllArgs(List.of(Commandline.translateCommandline(task.getArgs())));
        }

        return builder.build();
    }
}
