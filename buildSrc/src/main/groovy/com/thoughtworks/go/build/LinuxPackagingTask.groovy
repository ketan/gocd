/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.build;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public abstract class LinuxPackagingTask extends DefaultTask {
  @Input
  String packageName
  @Input
  String packageDescription
  @Input
  def directories
  @Input
  def files
  @Input
  String version
  @Input
  String distVersion


  @OutputFile
  abstract public File getOutputFile()

  LinuxPackagingTask() {
    outputs.upToDateWhen { false }
  }

  @TaskAction
  public void perform() {
    prepareFileSystem()
    buildPackage()
  }

  protected List fpmOpts() {
    def cmd = []
    cmd += ['fpm']

//  cmd += ['--debug']
//  cmd += ['-e']
//  cmd += ['--debug-workspace']
    cmd += ['--conflicts', packageName.replace('go-', 'cruise-')]
    cmd += ['--replaces', packageName.replace('go-', 'cruise-')]
    cmd += ['--force']
    cmd += ['-s', 'dir']
    cmd += ['-C', buildRoot()]
    cmd += ['--name', packageName]
    cmd += ['--description', packageDescription]
    cmd += ['--version', version]
    cmd += ['--iteration', distVersion]
    cmd += ['--license', 'Apache-2.0']
    cmd += ['--vendor', 'ThoughtWorks, Inc.']
    cmd += ['--category', 'Development/Build Tools']
    cmd += ['--architecture', 'all']
    cmd += ['--maintainer', 'ThoughtWorks, Inc.']
    cmd += ['--url', 'https://go.cd']
    cmd += ['--before-upgrade', project.file('linux-shared/before-upgrade.sh.erb')]
    cmd += ['--before-install', project.file('linux-shared/before-install.sh.erb')]
    cmd += ['--after-install', project.file('linux-shared/after-install.sh.erb')]
    cmd += ['--before-remove', project.file('linux-shared/before-remove.sh.erb')]
    cmd += ['--after-remove', project.file('linux-shared/after-remove.sh.erb')]
    cmd += ['--template-scripts']

    directories.each { dirName, permissions ->
      if (permissions.ownedByPackage) {
        cmd += ['--directories', dirName]
      }
    }

    files.each { fileName, permissions ->
      if (permissions.confFile) {
        cmd += ['--config-files', fileName.replaceAll(/^\//, '')]
      }
    }

    cmd
  }

  def buildRoot() {
    project.file("${project.buildDir}/${packageName}/${packageType()}/BUILD_ROOT")
  }

  final buildPackage() {
    project.exec {
      commandLine fpmOpts()
      workingDir project.convention.plugins.get("base").distsDir

      if (project.logger.isInfoEnabled()) {
        standardOutput = System.out
        errorOutput = System.err
      } else {
        standardOutput = new ByteArrayOutputStream()
        errorOutput = new ByteArrayOutputStream()
      }
    }
  }

  protected void prepareFileSystem() {
    buildRoot().deleteDir()
    buildRoot().mkdirs()
    project.convention.plugins.get("base").distsDir.mkdirs()
    // prepare the filesystem
    directories.each { dirName, permissions ->
      project.file("${buildRoot()}/${dirName}").mkdirs()
    }

    files.each { fileName, permissions ->
      project.copy {
        from permissions.source
        into project.file("${buildRoot()}/${new File(fileName).parentFile}")
        rename new File(permissions.source).name, new File(fileName).name
      }
    }

    File propertiesFile = project.fileTree(buildRoot()) { include("**/*/log4j.properties") }.files.first()

    def text = propertiesFile.getText().
        replaceAll(/go-agent\.log/, '/var/log/go-agent/go-agent.log').
        replaceAll(/go-server\.log/, '/var/log/go-agent/go-server.log').
        replaceAll(/go-shine\.log/, '/var/log/go-agent/go-shine.log')

    propertiesFile.write(text)

  }

}
