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

package com.thoughtworks.material.git.command.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ScriptGenerator {

    private final String mainClass;
    private final List<String> classPath;
    private final List<String> parameters;
    private String scriptName;

    public ScriptGenerator(String scriptName, String mainClass, List<String> classPath, List<String> parameters) {
        this.scriptName = scriptName;
        this.mainClass = mainClass;
        this.classPath = classPath;
        this.parameters = parameters;
    }

    public String buildScript() {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        shebang(out);

        String line = commandLine();

        if (isWindows()) {
            line += " %*";
        } else {
            line += " \"$@\"";
        }
        out.println(line);

        return buffer.toString();
    }

    private void shebang(PrintWriter out) {
        if (isWindows()) {
            out.println("@echo off");
        } else {
            out.println("#!/bin/sh");
        }
    }

    private boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private String commandLine() {
        StringBuilder cmd = new StringBuilder();
        cmd.append('\"').append(System.getProperty("java.home")).append(File.separatorChar).append("bin").append(File.separatorChar)
                .append("java\"")
//                .append(" -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
                .append(" -cp \"");
        boolean first = true;
        for (String p : classPath) {
            if (!first) {
                cmd.append(File.pathSeparatorChar);
            } else {
                first = false;
            }
            cmd.append(p);
        }
        cmd.append("\" ");
        cmd.append(mainClass);
        for (String p : parameters) {
            cmd.append(' ');
            cmd.append(p);
        }
        String line = cmd.toString();
        if (isWindows()) {
            line = line.replace('\\', '/');
        }
        return line;
    }

    public File write() throws IOException {
        File tempFile = File.createTempFile(scriptName, isWindows() ? ".bat" : ".sh");
        FileUtils.writeStringToFile(tempFile, buildScript(), StandardCharsets.UTF_8);
        if (!isWindows()) {
            tempFile.setExecutable(true);
        }
        return tempFile;
    }
}
