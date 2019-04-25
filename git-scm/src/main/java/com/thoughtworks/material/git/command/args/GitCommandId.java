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

package com.thoughtworks.material.git.command.args;

import com.thoughtworks.material.git.command.config.GitConfig;

public enum GitCommandId {
    Get_Current_Revision() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"log", "-1", "--pretty=format:%H"};
        }
    },

    Get_Version() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"version"};
        }
    },

    Contains_Revision() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String revision = contextualArgs[0];
            return new String[]{"branch", "-r", "--contains", revision};
        }
    },

    Reset_Hard_To_Revision() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String revision = contextualArgs[0];
            return new String[]{"reset", "--hard", revision};
        }
    },

    Clean_Unversioned_Files() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String cleanFlag = contextualArgs[0];
            return new String[]{"clean", cleanFlag};
        }
    },

    Clean_Unversioned_Files_In_All_Submodules() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String cleanFlag = contextualArgs[0];
            return new String[]{"submodule", "foreach", "--recursive", "git", "clean", cleanFlag};
        }
    },

    Get_Current_Branch() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"rev-parse", "--abbrev-ref", "HEAD"};
        }
    },

    Checkout_Remote_Branch_To_Local() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            String branch = contextualArgs[0];
            String remoteBranch = contextualArgs[1];
            return new String[]{"checkout", "-b", branch, remoteBranch};
        }
    },

    Working_Repo_Url() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"config", "remote.origin.url"};
        }
    },

    Get_Submodule_Urls() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"config", "--get-regexp", "^submodule\\..+\\.url"};
        }
    },

    Sync_Submodule() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"submodule", "sync"};
        }
    },

    Sync_Submodule_Recursively() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"submodule", "foreach", "--recursive", "git", "submodule", "sync"};
        }
    },

    Submodule_Status() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"submodule", "status"};
        }
    },

    Initialize_Submodule() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"submodule", "init"};
        }
    },

    Update_Submodule_With_Depth() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String depth = contextualArgs[0];
            return new String[]{"submodule", "update", "--depth=" + depth};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Update_Submodule() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"submodule", "update"};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Change_Submodule_Url() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String submoduleName = contextualArgs[0];
            final String urlToChangeTo = contextualArgs[1];

            return new String[]{"config", "--file", ".gitmodules", "submodule." + submoduleName + ".url", urlToChangeTo};
        }
    },

    Change_Submodule_Name_In_Git_Modules() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String folder = contextualArgs[0];
            final String nameToChangeTo = contextualArgs[1];

            return new String[]{"config", "--file", ".gitmodules", "--rename-section", "submodule." + folder, "submodule." + nameToChangeTo};
        }
    },


    Add_Submodule() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String repoUrl = contextualArgs[0];
            final String folder = contextualArgs[1];

            return new String[]{"submodule", "add", repoUrl, folder};
        }
    },

    Add_Git_Modules() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"add", ".gitmodules"};
        }
    },

    Remove_Files_From_Index() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String folderName = contextualArgs[0];
            return new String[]{"rm", "--cached", folderName};
        }
    },

    Checkout_Modified_Files_In_Submodules() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"submodule", "foreach", "--recursive", "git", "checkout", "."};
        }
    },

    GC() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"gc", "--auto"};
        }
    },

    Init() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"init"};
        }
    },

    Log() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            String[] finalArgs = new String[contextualArgs.length + 1];
            finalArgs[0] = "log";

            System.arraycopy(contextualArgs, 0, finalArgs, 1, contextualArgs.length);
            return finalArgs;
        }
    },

    Fetch() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"fetch", "origin", "--prune", "--recurse-submodules=no"};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Unshallow() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String depth = contextualArgs[0];
            return new String[]{"fetch", "origin", String.format("--depth=%s", depth)};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Push() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"push"};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Pull() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"pull"};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Add() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String fileToAdd = contextualArgs[0];
            return new String[]{"add", fileToAdd};
        }
    },

    Commit() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String commitMessage = contextualArgs[0];
            return new String[]{"commit", "-m", commitMessage};
        }
    },

    Diff_Tree() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String treeNode = contextualArgs[0];
            return new String[]{"diff-tree", "--name-status", "--root", "-r", treeNode};
        }
    },

    Check_Connection() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            return new String[]{"ls-remote", config.url(), "refs/heads/" + config.branch()};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Remove_Section_From_Config() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String sectionToRemove = contextualArgs[0];
            return new String[]{"config", "--remove-section", sectionToRemove};
        }
    },

    Remove_Section_From_Module_Config() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String folderName = contextualArgs[0];
            return new String[]{"config", "-f", ".gitmodules", "--remove-section", "submodule." + folderName};
        }
    },

    Clone() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String branch = contextualArgs[0];
            final String branchArg = String.format("--branch=%s", branch);

            return new String[]{"clone", branchArg, config.url(), config.getWorkingDir().getAbsolutePath()};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Clone_With_No_Checkout() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String branch = contextualArgs[0];
            final String branchArg = String.format("--branch=%s", branch);

            return new String[]{"clone", branchArg, config.url(), config.getWorkingDir().getAbsolutePath(), "--no-checkout"};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    },

    Clone_With_Depth() {
        @Override
        public String[] generateArgs(GitConfig config, String... contextualArgs) {
            final String branch = contextualArgs[0];
            final String branchArg = String.format("--branch=%s", branch);
            final String depthArg = String.format("--depth=%s", contextualArgs[1]);

            return new String[]{"clone", branchArg, config.url(), config.getWorkingDir().getAbsolutePath(), depthArg};
        }

        @Override
        public boolean requiresRemoteConnection() {
            return true;
        }
    };

    public String[] generateArgs(GitConfig config, String... contextualArgs) {
        // this should never be called as every individual command id has it's own implementation of generating arguments
        // given the GitConfig and set of contextual arguments.
        throw new IllegalStateException("generateArgs called from GitCommandId");
    }

    public boolean requiresRemoteConnection() {
        return false;
    }
}
