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

package com.thoughtworks.go.agent.meta;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Setter
@Getter
@Accessors(chain = true)
@ToString
@Slf4j
public class AgentMeta {
    private String uuid;
    private String hostname;
    private String location;
    private Long usableSpace;
    private String operationSystem;
    private String ipAddress;

    public AgentMeta refreshUsableSpaceInPipelinesDir() {
        File file = new File(location, "pipelines");
        if (!file.exists()) {
            log.warn("the [{}] should be created when agent starts up, but it seems missing at the moment. Cruise should be able to automatically create it later", file.getAbsolutePath());
        }
        long usableSpace = file.getUsableSpace();

        // See https://bugs.openjdk.java.net/browse/JDK-8162520
        return setUsableSpace(usableSpace < 0 ? Long.MAX_VALUE : usableSpace);
    }
}
