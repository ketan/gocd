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

package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.util.command.SecretString;

import java.io.File;
import java.util.List;
import java.util.Map;

public class GitCommandFactory {

    public static final String USE_OLD_EXECUTION_TOGGLE = "toggle.git.use.old.execution";

    public static GitCommand create(String materialFingerprint, File workingDir, String branch, boolean isSubmodule,
                                    Map<String, String> environment, List<SecretString> secrets) {
        String useOldExecution = System.getProperty(USE_OLD_EXECUTION_TOGGLE);
        if("Y".equalsIgnoreCase(useOldExecution)) {
            return new GitCommandOld(materialFingerprint, workingDir, branch, isSubmodule, environment, secrets);
        }
        return new GitCommandNew(materialFingerprint, workingDir, branch, isSubmodule, environment, secrets);
    }

}
