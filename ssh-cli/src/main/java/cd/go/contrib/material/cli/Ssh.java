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

import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Ssh {

    private static final String SSH_KEY = "PR_SSH_KEY";
    private static final String SSH_PASSWORD = "PR_SSH_PASSWORD";
    private static final String SSH_KEY_PASSPHRASE = "PR_SSH_KEY_PASSPHRASE";
    private static final String LOG_FILE_NAME = System.getenv().get("GIT_SSH_TRACE");
    private static final boolean LOGGING_ENABLED = System.getenv().containsKey("GIT_SSH_TRACE");

    private final SshArgParser argParser;

    public Ssh(SshArgParser argParser) {
        this.argParser = argParser;
    }

    public static void main(String[] args) {
        SshArgParser argParser = new SshArgParser(args);
        argParser.parse();

        int exitCode = 255;
        try {
            exitCode = new Ssh(argParser).connect();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(255);
        }

        System.exit(exitCode);
    }

    private int connect() throws FileNotFoundException, JSchException {
        int exitCode;

        JSch jSch = new JSch();
        SshLogger logger = logger();

        JSch.setLogger(logger);

        if (System.getenv().containsKey(SSH_KEY)) {
            byte[] privateKey = getEnvOrDie(SSH_KEY).getBytes(StandardCharsets.UTF_8);
            if (logger.isEnabled(Logger.DEBUG)) {
                logger.log(Logger.DEBUG, "Using SSH key\n" + getEnvOrDie(SSH_KEY));
            }
            byte[] passphrase = null;

            if (System.getenv().containsKey(SSH_KEY_PASSPHRASE)) {
                passphrase = getEnvOrDie(SSH_KEY_PASSPHRASE).getBytes(StandardCharsets.UTF_8);
                if (logger.isEnabled(Logger.DEBUG)) {
                    logger.log(Logger.DEBUG, "Passphrase\n" + getEnvOrDie(SSH_KEY_PASSPHRASE));
                }
            }

            jSch.addIdentity("SshKey", privateKey, null, passphrase);
        }
        Session session = jSch.getSession(argParser.username(), argParser.hostname(), argParser.port());
        if (System.getenv().containsKey(SSH_PASSWORD)) {
            session.setPassword(getEnvOrDie(SSH_PASSWORD));
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.setTimeout(30000);
        session.connect();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(argParser.command());
        channel.setInputStream(System.in);
        channel.setOutputStream(System.out);
        channel.setErrStream(System.err);
        channel.connect();

        while (true) {
            if (channel.isClosed()) {
                exitCode = channel.getExitStatus();
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ee) {
            }
        }

        try {
            channel.disconnect();
        } catch (Exception ignore) {
        }

        try {
            session.disconnect();
        } catch (Exception ignore) {
        }

        return exitCode;
    }

    private static SshLogger logger() throws FileNotFoundException {
        OutputStream outputStream = new NullOutputStream();

        if (LOGGING_ENABLED) {
            outputStream = new FileOutputStream(LOG_FILE_NAME, true);
        }

        PrintStream stream = new PrintStream(outputStream, true);
        return new SshLogger(stream, LOGGING_ENABLED);
    }

    private static String getEnvOrDie(String key) {
        if (System.getenv().containsKey(key)) {
            return System.getenv(key);
        } else {
            throw new IllegalStateException("Variable " + key + " is not found!");
        }
    }

    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {

        }
    }
}