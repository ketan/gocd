/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.SCMCommand;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.command.*;
import com.thoughtworks.material.git.command.args.GitCommandId;
import com.thoughtworks.material.git.command.config.GitConfig;
import com.thoughtworks.material.git.command.exceptions.GitCommandExecutionException;
import com.thoughtworks.material.git.command.executors.GitCommandResult;
import com.thoughtworks.material.git.command.executors.GitProcessExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.config.materials.git.GitMaterial.UNSHALLOW_TRYOUT_STEP;
import static com.thoughtworks.go.domain.materials.ModifiedAction.parseGitAction;
import static com.thoughtworks.go.util.DateUtils.formatRFC822;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static com.thoughtworks.material.git.command.args.GitCommandId.*;

public class GitCommandNew extends SCMCommand implements GitCommand {
    private static final Logger LOG = LoggerFactory.getLogger(GitCommandNew.class);

    private static final Pattern GIT_SUBMODULE_STATUS_PATTERN = Pattern.compile("^.[0-9a-fA-F]{40} (.+?)( \\(.+\\))?$");
    private static final Pattern GIT_SUBMODULE_URL_PATTERN = Pattern.compile("^submodule\\.(.+)\\.url (.+)$");
    private static final Pattern GIT_DIFF_TREE_PATTERN = Pattern.compile("^(.)\\s+(.+)$");
    private static final String GIT_CLEAN_KEEP_IGNORED_FILES_FLAG = "toggle.agent.git.clean.keep.ignored.files";

    private final File workingDir;
    private final List<SecretString> secrets;
    private final String branch;
    private final boolean isSubmodule;
    private Map<String, String> environment;

    GitCommandNew(String materialFingerprint, File workingDir, String branch, boolean isSubmodule, Map<String,
            String> environment, List<SecretString> secrets) {
        super(materialFingerprint);
        this.workingDir = workingDir;
        this.secrets = secrets != null ? secrets : new ArrayList<>();
        this.branch = StringUtils.isBlank(branch) ? GitMaterialConfig.DEFAULT_BRANCH : branch;
        this.isSubmodule = isSubmodule;
        this.environment = environment;
    }

    @Override
    public int cloneWithNoCheckout(ConsoleOutputStreamConsumer outputStreamConsumer, String url) {
        int returnVal;
        try {
            GitCommandResult result = executeCommand(Clone_With_No_Checkout, workingDir, url, outputStreamConsumer, branch);
            returnVal = result.returnValue();
        } catch (Exception e) {
            returnVal = 1;
        }
        return returnVal;
    }

    @Override
    public int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url) {
        return clone(outputStreamConsumer, url, Integer.MAX_VALUE);
    }

    // Clone repository from url with specified depth.
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as full clone
    @Override
    public int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url, Integer depth) {
        int returnVal;
        try {
            GitCommandResult result;
            if (depth < Integer.MAX_VALUE) {
                result = executeCommand(Clone_With_Depth, workingDir, url, outputStreamConsumer, branch, String.valueOf(depth));
            } else {
                result = executeCommand(Clone, workingDir, url, outputStreamConsumer, branch);
            }
            returnVal = result.returnValue();
        } catch (Exception e) {
            returnVal = 1;
        }
        return returnVal;
    }

    @Override
    public List<Modification> latestModification() {
        return gitLog("-1", "--date=iso", "--no-decorate", "--pretty=medium", "--no-color", remoteBranch());

    }

    @Override
    public List<Modification> modificationsSince(Revision revision) {
        return gitLog("--date=iso", "--pretty=medium", "--no-decorate", "--no-color", String.format("%s..%s", revision.getRevision(), remoteBranch()));
    }

    private List<Modification> gitLog(String... args) {
        // Git log will only show changes before the currently checked out revision
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

        try {
            if (!isSubmodule) {
                fetch(outputStreamConsumer);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Working directory: %s\n%s", workingDir, outputStreamConsumer.getStdError()), e);
        }

        GitCommandResult result = executeCommand(Log, workingDir, outputStreamConsumer, args);

        GitModificationParser parser = new GitModificationParser();
        List<Modification> modifications = parser.parse(result.getOutputLines());
        for (Modification modification : modifications) {
            addModifiedFiles(modification);
        }
        return modifications;
    }

    private void addModifiedFiles(Modification mod) {
        GitCommandResult processResult = diffTree(mod.getRevision());
        List<String> result = processResult.getOutputLines();

        for (String resultLine : result) {
            // First line is the node
            if (resultLine.equals(mod.getRevision())) {
                continue;
            }

            Matcher m = matchResultLine(resultLine);
            if (!m.find()) {
                // TODO : Implement ConsoleResult.outputForDisplayAsString() & ConsoleResult.replaceSecretInfo() methods
                // in ProcessResult along with support for replacing arguments and secrets.
                bomb("Unable to parse git-diff-tree output line: " + resultLine + "\n"
                        + "From output:\n" + processResult.outputString());
            }
            mod.createModifiedFile(m.group(2), null, parseGitAction(m.group(1).charAt(0)));
        }
    }

    private Matcher matchResultLine(String resultLine) {
        return GIT_DIFF_TREE_PATTERN.matcher(resultLine);
    }

    @Override
    public void resetWorkingDir(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision, boolean shallow) {
        log(outputStreamConsumer, "Reset working directory %s", workingDir);
        cleanAllUnversionedFiles(outputStreamConsumer);
        removeSubmoduleSectionsFromGitConfig(outputStreamConsumer);
        resetHard(outputStreamConsumer, revision);
        checkoutAllModifiedFilesInSubmodules(outputStreamConsumer);
        updateSubmoduleWithInit(outputStreamConsumer, shallow);
        cleanAllUnversionedFiles(outputStreamConsumer);
    }

    private void checkoutAllModifiedFilesInSubmodules(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Removing modified files in submodules");
        executeCommand(Checkout_Modified_Files_In_Submodules, workingDir);
    }

    private void cleanAllUnversionedFiles(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Cleaning all unversioned files in working copy");
        cleanUnversionedFilesInAllSubmodules();
        cleanUnversionedFiles(workingDir);
    }

    private String gitCleanArgs() {
        if ("Y".equalsIgnoreCase(System.getProperty(GIT_CLEAN_KEEP_IGNORED_FILES_FLAG))) {
            LOG.info("{} = Y. Using old behaviour for clean using `-dff`", GIT_CLEAN_KEEP_IGNORED_FILES_FLAG);
            return "-dff";
        } else {
            return "-dffx";
        }
    }

    private void printSubmoduleStatus(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Git sub-module status");
        executeCommand(Submodule_Status, workingDir, outputStreamConsumer);
    }

    @Override
    public void resetHard(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision) {
        log(outputStreamConsumer, "Updating working copy to revision " + revision.getRevision());
        executeCommand(Reset_Hard_To_Revision, workingDir, outputStreamConsumer, revision.getRevision());
    }

    private CommandLine git(Map<String, String> environment) {
        CommandLine git = CommandLine.createCommandLine("git").withEncoding("UTF-8");
        git.withNonArgSecrets(secrets);
        return git.withEnv(environment);
    }

    @Override
    public void fetchAndResetToHead(ConsoleOutputStreamConsumer outputStreamConsumer) {
        fetchAndResetToHead(outputStreamConsumer, false);
    }

    @Override
    public void fetchAndResetToHead(ConsoleOutputStreamConsumer outputStreamConsumer, boolean shallow) {
        fetch(outputStreamConsumer);
        resetWorkingDir(outputStreamConsumer, new StringRevision(remoteBranch()), shallow);
    }

    @Override
    public void updateSubmoduleWithInit(ConsoleOutputStreamConsumer outputStreamConsumer, boolean shallow) {
        if (!gitSubmoduleEnabled()) {
            return;
        }
        log(outputStreamConsumer, "Updating git sub-modules");

        iniitializeSubmodule();

        submoduleSync();

        if (shallow && version(environment).supportsSubmoduleDepth()) {
            tryToShallowUpdateSubmodules();
        } else {
            updateSubmodule();
        }

        log(outputStreamConsumer, "Cleaning unversioned files and sub-modules");
        printSubmoduleStatus(outputStreamConsumer);
    }

    private void iniitializeSubmodule() {
        executeCommand(Initialize_Submodule, workingDir);
    }

    private void updateSubmodule() {
        executeCommand(Update_Submodule, workingDir);
    }

    private void tryToShallowUpdateSubmodules() {
        if (updateSubmoduleWithDepth(1)) {
            return;
        }
        LOG.warn("git submodule update with --depth=1 failed. Attempting again with --depth={}", UNSHALLOW_TRYOUT_STEP);
        if (updateSubmoduleWithDepth(UNSHALLOW_TRYOUT_STEP)) {
            return;
        }
        LOG.warn("git submodule update with depth={} failed. Attempting again with --depth=Integer.MAX", UNSHALLOW_TRYOUT_STEP);
        if (!updateSubmoduleWithDepth(Integer.MAX_VALUE)) {
            bomb("Failed to update submodule");
        }
    }

    private boolean updateSubmoduleWithDepth(int depth) {
        try {
            executeCommand(Update_Submodule_With_Depth, workingDir, String.valueOf(depth));
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private void cleanUnversionedFiles(File workingDir) {
        executeCommand(Clean_Unversioned_Files, workingDir, gitCleanArgs());
    }

    private void cleanUnversionedFilesInAllSubmodules() {
        executeCommand(Clean_Unversioned_Files_In_All_Submodules, workingDir, gitCleanArgs());
    }

    private void removeSubmoduleSectionsFromGitConfig(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Cleaning submodule configurations in .git/config");
        for (String submoduleFolder : submoduleUrls().keySet()) {
            configRemoveSection("submodule." + submoduleFolder);
        }
    }

    private void configRemoveSection(String section) {
        executeCommand(Remove_Section_From_Config, workingDir, section);
    }

    private boolean gitSubmoduleEnabled() {
        return new File(workingDir, ".gitmodules").exists();
    }

    @Override
    public UrlArgument workingRepositoryUrl() {
        GitCommandResult result = executeCommand(Working_Repo_Url, workingDir);
        return new UrlArgument(result.getOutputLines().get(0));
    }

    @Override
    public void checkConnection(UrlArgument repoUrl, String branch, Map<String, String> environment) {
        final GitConfig.Builder gitConfigBuilder = GitConfig.newBuilder();
        gitConfigBuilder.url(repoUrl.originalArgument()).branch(branch);
        GitConfig gitConfig = gitConfigBuilder.build();

        GitCommandResult result = GitProcessExecutor.create(Check_Connection, gitConfig)
                .environment(environment)
                .execute();

        List<String> outputLines = result.getOutputLines();
        if (!hasOnlyOneMatchingBranch(outputLines)) {
            throw new GitCommandExecutionException(String.format("The branch %s could not be found.", branch));
        }
    }

    private static boolean hasOnlyOneMatchingBranch(List<String> branchList) {
        return (branchList.size() == 1);
    }

    @Override
    public GitVersion version(Map<String, String> map) {
        GitConfig gitConfig = GitConfig.newBuilder().build();

        GitCommandResult result = GitProcessExecutor.create(Get_Version, gitConfig).environment(map).execute();
        return GitVersion.parse(result.outputString());
    }

    @Override
    public void add(File fileToAdd) {
        executeCommand(Add, workingDir, fileToAdd.getName());
    }

    @Override
    public void commit(String message) {
        executeCommand(Commit, workingDir, message);
    }

    @Override
    public void push() {
        executeCommand(Push, workingDir);
    }

    @Override
    public void pull() {
        executeCommand(Pull, workingDir);
    }

    void commitOnDate(String message, Date commitDate) {
        GitConfig gitConfig = GitConfig.newBuilder().workingDir(workingDir.getAbsolutePath()).build();
        GitProcessExecutor.create(Commit, gitConfig)
                .additionalArgs(message)
                .environment(Collections.singletonMap("GIT_AUTHOR_DATE", formatRFC822(commitDate)))
                .execute();
    }

    public void checkoutRemoteBranchToLocal() {
        executeCommand(Checkout_Remote_Branch_To_Local, workingDir, branch, remoteBranch());
    }

    private String remoteBranch() {
        return "origin/" + branch;
    }

    @Override
    public void fetch(ConsoleOutputStreamConsumer outputStreamConsumer) {
        executeCommand(Fetch, workingDir, outputStreamConsumer);
        executeCommand(GC, workingDir, outputStreamConsumer);
    }

    private LogOutputStream errorConsumer(ConsoleOutputStreamConsumer outputStreamConsumer) {
        return new LogOutputStream() {
            @Override
            protected void processLine(String line) {
                outputStreamConsumer.errOutput(line);
            }
        };
    }

    private LogOutputStream outputConsumer(ConsoleOutputStreamConsumer outputStreamConsumer) {
        return new LogOutputStream() {
            @Override
            protected void processLine(String line) {
                outputStreamConsumer.stdOutput(line);
            }
        };
    }

    // Unshallow a shallow cloned repository with "git fetch --depth n".
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as infinite -- fully unshallow
    // https://git-scm.com/docs/git-fetch-pack
    @Override
    public void unshallow(ConsoleOutputStreamConsumer outputStreamConsumer, Integer depth) {
        log(outputStreamConsumer, "Unshallowing repository with depth %d", depth);
        executeCommand(Unshallow, workingDir, String.valueOf(depth));
    }

    @Override
    public void init() {
        executeCommand(Init, workingDir);
    }

    List<String> submoduleFolders() {
        GitCommandResult result = executeCommand(Submodule_Status, workingDir);
        return submoduleFolders(result.getOutputLines());
    }

    private GitCommandResult diffTree(String node) {
        return executeCommand(Diff_Tree, workingDir, node);
    }

    @Override
    public void submoduleAdd(String repoUrl, String submoduleName, String folder) {
        GitConfig.Builder gitConfigBuilder = GitConfig.newBuilder().workingDir(workingDir.getAbsolutePath());
        GitConfig gitConfig = gitConfigBuilder.build();
        GitProcessExecutor.create(Add_Submodule, gitConfig)
                .additionalArgs(repoUrl, folder)
                .execute();
        GitProcessExecutor.create(Change_Submodule_Name_In_Git_Modules, gitConfig)
                .additionalArgs(folder, submoduleName)
                .execute();
        GitProcessExecutor.create(Add_Git_Modules, gitConfig)
                .execute();
    }

    @Override
    public void submoduleRemove(String folderName) {
        configRemoveSection("submodule." + folderName);

        GitConfig.Builder gitConfigBuilder = GitConfig.newBuilder().workingDir(workingDir.getAbsolutePath());
        GitConfig gitConfig = gitConfigBuilder.build();

        GitProcessExecutor.create(Remove_Section_From_Module_Config, gitConfig).additionalArgs(folderName).execute();
        GitProcessExecutor.create(Add_Git_Modules, gitConfig).execute();
        GitProcessExecutor.create(Remove_Files_From_Index, gitConfig).additionalArgs(folderName).execute();

        FileUtils.deleteQuietly(new File(workingDir, folderName));
    }

    @Override
    public String currentRevision() {
        GitConfig.Builder gitConfigBuilder = GitConfig.newBuilder().workingDir(workingDir.getAbsolutePath());
        GitConfig gitConfig = gitConfigBuilder.build();

        return GitProcessExecutor.create(Get_Current_Revision, gitConfig)
                .execute()
                .outputString();
    }

    private List<String> submoduleFolders(List<String> submoduleLines) {
        ArrayList<String> submoduleFolders = new ArrayList<>();
        for (String submoduleLine : submoduleLines) {
            Matcher m = GIT_SUBMODULE_STATUS_PATTERN.matcher(submoduleLine);
            if (!m.find()) {
                bomb("Unable to parse git-submodule output line: " + submoduleLine + "\n"
                        + "From output:\n"
                        + StringUtils.join(submoduleLines, "\n"));
            }
            submoduleFolders.add(m.group(1));
        }
        return submoduleFolders;
    }

    @Override
    public Map<String, String> submoduleUrls() {
        GitConfig gitConfig = GitConfig.newBuilder().workingDir(workingDir.getAbsolutePath()).build();

        List<String> submoduleList = GitProcessExecutor.create(Get_Submodule_Urls, gitConfig)
                .failOnNonZeroReturn(false)
                .execute()
                .getOutputLines();

        HashMap<String, String> submoduleUrls = new HashMap<>();
        for (String submoduleLine : submoduleList) {
            Matcher m = GIT_SUBMODULE_URL_PATTERN.matcher(submoduleLine);
            if (!m.find()) {

            }
            submoduleUrls.put(m.group(1), m.group(2));
        }
        return submoduleUrls;
    }

    @Override
    public String getCurrentBranch() {
        GitCommandResult result = executeCommand(Get_Current_Branch, workingDir);
        return result.outputString().trim();
    }

    @Override
    public void changeSubmoduleUrl(String submoduleName, String newUrl) {
        executeCommand(Change_Submodule_Url, workingDir, submoduleName, newUrl);
    }

    @Override
    public void submoduleSync() {
        executeCommand(Sync_Submodule, workingDir);
        executeCommand(Sync_Submodule_Recursively, workingDir);
    }

    @Override
    public boolean isShallow() {
        return new File(workingDir, ".git/shallow").exists();
    }

    @Override
    public boolean containsRevisionInBranch(Revision revision) {
        GitCommandResult result = null;
        try {
            result = executeCommand(Contains_Revision, workingDir, revision.getRevision());
            return (result.outputString()).contains(remoteBranch());
        } catch (Exception e) {
            return false;
        }
    }

    private GitConfig getGitConfig(File workingDir, String url) {
        GitConfig.Builder builder = GitConfig.newBuilder();
        if (url != null && url.trim().length() > 0) {
            return builder.workingDir(workingDir.getAbsolutePath()).url(url).build();
        } else {
            return builder.workingDir(workingDir.getAbsolutePath()).build();
        }
    }

    private void log(ConsoleOutputStreamConsumer outputStreamConsumer, String message, Object... args) {
        LOG.debug(String.format(message, args));
        outputStreamConsumer.stdOutput(String.format("[GIT] " + message, args));
    }

    private GitCommandResult executeCommand(GitCommandId commandId, File workingDir, String url,
                                            ConsoleOutputStreamConsumer outputStreamConsumer,
                                            String... additionalArgs) {
        GitConfig gitConfig = getGitConfig(workingDir, url);
        SafeOutputStreamConsumer safeOutputStreamConsumer = new SafeOutputStreamConsumer(outputStreamConsumer);
        safeOutputStreamConsumer.addSecrets(this.secrets);
        return GitProcessExecutor.create(commandId, gitConfig)
                .additionalArgs(additionalArgs)
                .maskSecretsFn((str) -> {
                    for (SecretString secret : secrets) {
                        str = secret.replaceSecretInfo(str);
                    }
                    return str;
                })
                .stdOutConsumer(outputConsumer(safeOutputStreamConsumer))
                .stdErrConsumer(errorConsumer(safeOutputStreamConsumer))
                .environment(environment)
                .execute();
    }

    private GitCommandResult executeCommand(GitCommandId commandId, File workingDir, String... additionalArgs) {
        return executeCommand(commandId, workingDir, null, inMemoryConsumer(), additionalArgs);
    }

    private GitCommandResult executeCommand(GitCommandId commandId, File workingDir,
                                            ConsoleOutputStreamConsumer outputStreamConsumer,
                                            String... additionalArgs) {
        return executeCommand(commandId, workingDir, null, outputStreamConsumer, additionalArgs);
    }
}
