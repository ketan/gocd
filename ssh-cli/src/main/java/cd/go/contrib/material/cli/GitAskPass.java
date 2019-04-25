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

public class GitAskPass {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected only one argument");
        }

        if (args[0].toLowerCase().startsWith("username")) {
            System.out.println(getEnvOrEmpty("PR_GIT_ASKPASS_USER"));
        } else if (args[0].toLowerCase().startsWith("password")) {
            System.out.println(getEnvOrEmpty("PR_GIT_ASKPASS_PASS"));
        } else {
            throw new IllegalArgumentException("Could not understand argument " + args[0]);
        }
    }

    private static String getEnvOrEmpty(String key) {
        if (System.getenv().containsKey(key)) {
            return System.getenv(key);
        } else {
            return "No [" + key + "] is set!";
        }
    }
}
