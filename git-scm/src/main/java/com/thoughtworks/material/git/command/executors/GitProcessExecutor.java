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

import com.thoughtworks.material.git.command.args.GitCommandId;
import com.thoughtworks.material.git.command.config.GitConfig;
import com.thoughtworks.material.git.command.exceptions.GitCommandExecutionException;
import com.thoughtworks.material.git.command.utils.ScriptGenerator;
import com.thoughtworks.material.git.command.utils.Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class GitProcessExecutor {

    public static final String SSH_CLI_JAR_FILE_PATH = "ssh_cli_jar_file_path";

    private String[] additionalArgs;
    private GitCommandId commandId;
    private final String sshCliPath;
    private GitConfig gitConfig;

    private LogOutputStream clientSpecifiedStdOutConsumer;
    private LogOutputStream clientSpecifiedStdErrConsumer;

    private boolean failOnNonZeroReturn = true;
    private Map<String, String> env = new HashMap<>();

    private List<String> stdOutputLines = new ArrayList<>();
    private List<String> stdErrLines = new ArrayList<>();

    private static final Logger LOG = LoggerFactory.getLogger(GitProcessExecutor.class);
    private Function<String, String> maskFn = (str) -> str;

    private GitProcessExecutor(GitCommandId commandId, GitConfig gitConfig) {
        this.commandId = commandId;

        String path = System.getProperty(SSH_CLI_JAR_FILE_PATH);
        this.sshCliPath = (path == null ? "ssh-cli.jar" : path);

        this.gitConfig = gitConfig;
    }

    public static GitProcessExecutor create(GitCommandId commandId, GitConfig gitConfig) {
        return new GitProcessExecutor(commandId, gitConfig);
    }

    public GitProcessExecutor additionalArgs(String... args) {
        this.additionalArgs = args;
        return this;
    }

    public GitProcessExecutor stdOutConsumer(LogOutputStream stdOutConsumer) {
        this.clientSpecifiedStdOutConsumer = stdOutConsumer;
        return this;
    }

    public GitProcessExecutor stdErrConsumer(LogOutputStream stdErrConsumer) {
        this.clientSpecifiedStdErrConsumer = stdErrConsumer;
        return this;
    }

    public GitProcessExecutor failOnNonZeroReturn(boolean choice) {
        this.failOnNonZeroReturn = choice;
        return this;
    }

    public GitProcessExecutor environment(Map<String, String> envMap) {
        if (this.env != null) {
            this.env.putAll(envMap);
        }
        return this;
    }

    public GitProcessExecutor maskSecretsFn(Function<String, String> maskFn) {
        this.maskFn = maskFn;
        return this;
    }

    public GitCommandResult execute() throws GitCommandExecutionException {
        logProcessExecutionStart();

        if (!gitConfig.getWorkingDir().exists()) {
            gitConfig.getWorkingDir().mkdirs();
        }

        File cliJar = null;
        File askPassExecutable = null;
        File sshExecutable = null;

        GitCommandResult result = null;
        try {
            ProcessExecutor pe = createAndInitializeSystemProcessExecutor();

            if (commandId.requiresRemoteConnection()) {
                cliJar = Util.copyResourceIntoTempFile(sshCliPath, "ssh-cli", ".jar");

                askPassExecutable = askpassExecutable(cliJar);
                sshExecutable = sshExecutable(cliJar);
                setEnvironmentVariables(pe, askPassExecutable, sshExecutable);
            }

            result = executeCommand(pe);

            if (result.failed()) {
                logAndThrowCommandExecutionException(result);
            }
        } catch (Exception e) {
            logAndThrowCommandExecutionException(getDefaultResult(), e);
        } finally {
            delete(cliJar);
            delete(askPassExecutable);
            delete(sshExecutable);
        }

        logProcessExecutionEnd();

        return result;
    }

    private GitCommandResult getDefaultResult() {
        return new GitCommandResult(convertToString(getStdOutputLines()), convertToString(getStdErrLines()));
    }

    private String convertToString(List<String> lines) {
        return StringUtils.join(lines, "\n");
    }

    private ArrayList<String> getCompleteArgList() {
        ArrayList<String> args = new ArrayList<>();
        args.add("git");
        args.addAll(Arrays.asList(commandId.generateArgs(gitConfig, this.additionalArgs)));
        return args;
    }

    private List<String> getStdOutputLines() {
        return stdOutputLines;
    }

    private List<String> getStdErrLines() {
        return stdErrLines;
    }

    private void setTracingEnvironmentVariables(ProcessExecutor pe) {
        LOG.debug("Tracing is enabled for process execution");
        pe.environment("GIT_TRACE", "/tmp/git-trace.log");
        pe.environment("GIT_SSH_TRACE", "/tmp/git-ssh-trace.log");
        pe.environment("GIT_CURL_VERBOSE", "1");
    }

    private ProcessExecutor createAndInitializeSystemProcessExecutor() {
        ProcessExecutor pe = new ProcessExecutor(getCompleteArgList()).readOutput(true)
                .directory(this.gitConfig.getWorkingDir());
        if (!env.isEmpty()) {
            pe.environment(env);
        }

        redirectProcessStdOutAndStdErrorToClientSpecifiedConsumers(pe);

        if (gitConfig.enableTracing()) {
            setTracingEnvironmentVariables(pe);
        }

        redirectProcessStdOutAndStdErrorToDefaultConsumers(pe);
        return pe;
    }

    private void redirectProcessStdOutAndStdErrorToClientSpecifiedConsumers(ProcessExecutor pe) {
        if (clientSpecifiedStdOutConsumer != null) {
            LOG.debug("Redirecting process execution output to specified output stream");
            pe.redirectOutput(clientSpecifiedStdOutConsumer);
        }

        if (clientSpecifiedStdErrConsumer != null) {
            LOG.debug("Redirecting process execution errors to specified error stream");
            pe.redirectError(clientSpecifiedStdErrConsumer);
        }
    }

    private void redirectProcessStdOutAndStdErrorToDefaultConsumers(ProcessExecutor pe) {
        pe.redirectOutputAlsoTo(newStdOutConsumer());
        pe.redirectErrorAlsoTo(newStdErrConsumer());
    }

    private GitCommandResult executeCommand(ProcessExecutor pe) throws IOException, InterruptedException, TimeoutException {
        ProcessResult processResult = pe.execute();

        return new GitCommandResult(processResult, convertToString(getStdOutputLines()),
                convertToString(getStdErrLines()), maskFn, failOnNonZeroReturn);
    }

    private LogOutputStream newStdOutConsumer() {
        return new LogOutputStream() {
            @Override
            protected void processLine(String line) {
                stdOutputLines.add(line);
            }
        };
    }

    private LogOutputStream newStdErrConsumer() {
        return new LogOutputStream() {
            @Override
            protected void processLine(String line) {
                stdErrLines.add(line);
            }
        };
    }

    private void setEnvironmentVariables(ProcessExecutor pe, File askPassExecutable, File sshExecutable) {
        pe.environment("GIT_SSH_VARIANT", "ssh");

        if (gitConfig.hasPassword()) {
            pe.environment("PR_GIT_ASKPASS_USER", gitConfig.username());
            pe.environment("PR_GIT_ASKPASS_PASS", gitConfig.password());
        }

        pe.environment("GIT_ASKPASS", askPassExecutable.getAbsolutePath());
        pe.environment("GIT_SSH", sshExecutable.getAbsolutePath());

        setSshKeyEnvironmentVariables(pe);

        if (gitConfig.hasPassword()) {
            pe.environment("PR_SSH_PASSWORD", gitConfig.password());
        }

    }

    private void setSshKeyEnvironmentVariables(ProcessExecutor pe) {
        if (gitConfig.hasSshKey()) {
            pe.environment("PR_SSH_KEY", gitConfig.sshKey());

            if (gitConfig.hasSshKeyPassphrase()) {
                pe.environment("PR_SSH_KEY_PASSPHRASE", gitConfig.sshKeyPassphrase());
            }
        }
    }

    private File sshExecutable(File cliJar) throws IOException {
        return new ScriptGenerator("git-ssh", "cd.go.contrib.material.cli.Ssh", singletonList(cliJar.getAbsolutePath()), emptyList()).write();
    }

    private File askpassExecutable(File cliJar) throws IOException {
        return new ScriptGenerator("git-ask-pass", "cd.go.contrib.material.cli.GitAskPass", singletonList(cliJar.getAbsolutePath()), emptyList()).write();
    }

    private void delete(File file) {
        if (file != null && file.exists()) {
            file.deleteOnExit();
            file.delete();
        }
    }

    private void logAndThrowCommandExecutionException(GitCommandResult result, Exception e) {
        String msg = "Error while executing the command [" + getCommandForDisplay() + "]. "
                + "Error detail is [" + e.getMessage() + "]" + "\n"
                + result.describe();
        LOG.error(msg, e);
        throw new GitCommandExecutionException(msg, e);
    }

    private void logAndThrowCommandExecutionException(GitCommandResult result) {
        String msg = "Error while executing the command [" + getCommandForDisplay() + "]" + "\n" + result.describe();
        LOG.error(msg);
        throw new GitCommandExecutionException(msg);
    }

    private void logProcessExecutionStart() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing command [{}]...", getCommandForDisplay());
        }
    }

    private void logProcessExecutionEnd() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Command [{}] executed successfully", getCommandForDisplay());
        }
    }

    private String getCommandForDisplay() {
        return StringUtils.join(getCompleteArgList(), " ");
    }
}
