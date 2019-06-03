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

package com.thoughtworks.go.scm.git;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

@Slf4j
public class GitCLI {

    private static final File TEMP_DIR = new File("data/git-cli");

    static {
        FileUtils.deleteQuietly(TEMP_DIR);
        // cleanup temp dir on exit, if there are any files left behind
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(TEMP_DIR)));
    }

    private final ExecuteStreamHandler executeStreamHandler;

    public GitCLI(ExecuteStreamHandler executeStreamHandler) {
        this.executeStreamHandler = executeStreamHandler;
    }


    public static File createTempFile(String prefix, String extension) throws IOException {
        TEMP_DIR.mkdirs();

        if (!TEMP_DIR.exists()) {
            throw new IOException("Unable to create temp directory: " + TEMP_DIR);
        }

        return Files.createTempFile(TEMP_DIR.toPath(), prefix, extension, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))).toFile();
    }

    public static void deleteFilesWithWarning(File... files) {
        for (File file : files) {
            deleteFileWithWarning(file);
        }
    }

    private static void deleteFileWithWarning(File file) {
        if (file != null && !file.delete() && file.exists()) {
            log.warn("[WARNING] Temp file {} not deleted", file);
        }
    }

}
