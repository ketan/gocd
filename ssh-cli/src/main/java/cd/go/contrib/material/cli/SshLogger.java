/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package cd.go.contrib.material.cli;

import com.jcraft.jsch.Logger;

import java.io.PrintStream;
import java.util.Hashtable;

class SshLogger implements Logger {
    static Hashtable<Integer, String> name = new Hashtable<>();

    static {
        name.put(DEBUG, "DEBUG: ");
        name.put(INFO, "INFO: ");
        name.put(WARN, "WARN: ");
        name.put(ERROR, "ERROR: ");
        name.put(FATAL, "FATAL: ");
    }

    private final PrintStream stream;
    private final boolean enabled;

    public SshLogger(PrintStream writer, boolean enabled) {
        this.stream = writer;
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled(int level) {
        return enabled;
    }

    @Override
    public void log(int level, String message) {
        stream.print(name.get(level));
        stream.println(message);
    }
}
