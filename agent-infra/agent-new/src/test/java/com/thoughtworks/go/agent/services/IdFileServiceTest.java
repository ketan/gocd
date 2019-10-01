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

package com.thoughtworks.go.agent.services;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;


class IdFileServiceTest {
    private IdFileService idFileService;
    private String DATA = "data";
    private File idFile;

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        idFile = tempDir.resolve("temp.id").toFile();
        this.idFileService = new IdFileService(idFile) {
        };
    }

    @AfterEach
    void tearDown() {
        idFileService.delete();
    }

    @Test
    void shouldLoadDataFromFileIfFileIsPresent() throws IOException {
        FileUtils.write(idFile, DATA, StandardCharsets.UTF_8);
        assertThat(idFileService.load()).isEqualTo(DATA);
    }

    @Test
    void shouldStoreDataToFile() {
        idFileService.store("some-id");
        assertThat(idFileService.load()).isEqualTo("some-id");
    }

    @Test
    void shouldCheckIfDataPresent() {
        assertThat(idFile).doesNotExist();
        assertThat(idFileService.dataPresent()).isFalse();

        idFileService.store("some-id");
        assertThat(idFileService.dataPresent()).isTrue();
    }

    @Test
    void shouldDeleteFile() {
        idFileService.store("some-id");
        assertThat(idFile).exists();

        idFileService.delete();
        assertThat(idFile).doesNotExist();
    }
}