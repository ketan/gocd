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

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.protobufs.tasks.ExecProto;

import static java.lang.String.format;

public class TaskConverterFactory {
    public ExecProto toTask(Task task) {
        if (task instanceof ExecTask) {
            return new ExecBuilder().build((ExecTask) task);
        } else if (task instanceof AntTask) {
            return new AntBuilder().build((AntTask) task);
        } else if (task instanceof NantTask) {
            return new NAntBuilder().build((NantTask) task);
        } else if (task instanceof RakeTask) {
            return new RakeBuilder().build((RakeTask) task);
        }

        throw new IllegalArgumentException(format("Unknown task type %s.", task.getClass().getName()));
    }
}
