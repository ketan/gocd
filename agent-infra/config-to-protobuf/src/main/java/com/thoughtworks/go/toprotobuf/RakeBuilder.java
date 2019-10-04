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

import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.protobufs.tasks.ExecProto;
import org.apache.tools.ant.types.Commandline;

import java.util.List;

import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

class RakeBuilder implements Builder<RakeTask, ExecProto> {
    @Override
    public ExecProto build(RakeTask task) {
        ExecProto.Builder builder = ExecProto.newBuilder()
                .setCommand("rake");

        addWorkingDirIfPresent(task, builder);
        if (isNotBlank(task.getBuildFile())) {
            builder.addArgs("-f");
            builder.addArgs(separatorsToUnix(task.getBuildFile()));
        }

        if (isNotBlank(task.getTarget())) {
            builder.addAllArgs(List.of(Commandline.translateCommandline(task.getTarget())));
        }

        return builder.build();
    }
}
