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

package com.thoughtworks.material.git.command.executors;

import org.zeroturnaround.exec.ProcessResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GitCommandResult {
    private ProcessResult processResult;

    private String stdOut;
    private String stdErr;
    private Function<String, String> maskFn = (str) -> str;

    private boolean failOnNonZeroReturn = true;

    GitCommandResult(ProcessResult processResult, String stdOut, String stdErr, Function<String, String> maskFn, boolean failOnNonZeroReturn) {
        this.processResult = processResult;

        this.stdOut = stdOut;
        this.stdErr = stdErr;
        this.maskFn = maskFn;
        this.failOnNonZeroReturn = failOnNonZeroReturn;
    }

    boolean failed() {
        // Some git commands return non-zero return value for a "successful" command (e.g. git config --get-regexp)
        // In such a scenario, we can't simply rely on return value to tell whether a command is successful or not
        return failOnNonZeroReturn && returnValue() != 0;
    }

    GitCommandResult(String stdOut, String stdErr) {
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    private String getStdOut() {
        return maskFn.apply(stdOut);
    }

    private String getStdErr() {
        return maskFn.apply(stdErr);
    }

    public int returnValue(){
        if(processResult != null) {
            return processResult.getExitValue();
        }
        return 0;
    }

    public String describe() {
        return "--- EXIT CODE (" + returnValue() + ") ---\n"
                    + "--- STANDARD OUT ---\n" + getStdOut() + "\n"
                    + "--- STANDARD ERR ---\n" + getStdErr() + "\n"
                    + "---\n";
    }

    public List<String> getOutputLines(){
        return processResult == null ? new ArrayList<>() : processResult.getOutput().getLines();
    }

    public String outputString(){
        return processResult == null ? "" : processResult.outputString();
    }

}
