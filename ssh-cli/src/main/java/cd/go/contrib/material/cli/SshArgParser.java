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

import java.util.ArrayList;
import java.util.Arrays;

class SshArgParser {
    private final ArrayList<String> argList;

    private int port = 22;
    private String command = null;
    private String username = null;
    private String hostname = null;

    SshArgParser(String[] args) {
        this.argList = new ArrayList<>(Arrays.asList(args));
    }

    void parse() {
        if (argList.contains("-p")) {
            int indexOfPortArg = argList.indexOf("-p");
            argList.remove(indexOfPortArg);
            port = Integer.valueOf(argList.remove(indexOfPortArg));
        }

        String userHost = argList.remove(0);
        command = argList.remove(0);

        if (userHost.contains("@")) {
            String[] splits = userHost.split("@", 2);
            username = splits[0];
            hostname = splits[1];
        } else {
            hostname = userHost;
        }
    }

    int port() {
        return port;
    }

    String command() {
        return command;
    }

    String username() {
        return username;
    }

    String hostname() {
        return hostname;
    }
}
