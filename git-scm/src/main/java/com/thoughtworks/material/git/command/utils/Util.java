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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jetty.util.resource.Resource;

public class Util {

    public static File copyResourceIntoTempFile(String resourcePath, String tempFilePrefix, String tempfileExtension) {
        Resource resource = Resource.newClassPathResource(resourcePath);
        try (InputStream in = resource.getInputStream()) {
            File tempFile = File.createTempFile(tempFilePrefix, tempfileExtension);
            FileUtils.copyInputStreamToFile(in, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Could not find " + resourcePath, e);
        }
    }
}
