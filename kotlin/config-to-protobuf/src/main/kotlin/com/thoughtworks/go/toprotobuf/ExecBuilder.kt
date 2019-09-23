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

package com.thoughtworks.go.toprotobuf

import com.thoughtworks.go.config.ExecTask
import com.thoughtworks.go.protobufs.Exec
import org.apache.tools.ant.types.Commandline

class ExecBuilder : Builder<ExecTask, Exec> {
    override fun build(task: ExecTask): Exec {
        val builder = Exec.newBuilder()
                .setWorkingDir(task.workingDirectory())
                .setCommand(task.command)
                .addAllArgs(task.argList.toStringArray().asIterable())

        if (!task.args.isNullOrEmpty()) {
            builder.addAllArgs(Commandline.translateCommandline(task.args).asIterable())
        }

        return builder.build()
    }
}
