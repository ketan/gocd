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

package com.thoughtworks.go.agent

import com.thoughtworks.go.agent.io.OnIdleOutputStream
import com.thoughtworks.go.protobufs.Exec
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.StartedProcess
import org.zeroturnaround.exec.stream.NullOutputStream
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream
import org.zeroturnaround.process.PidUtil
import org.zeroturnaround.process.ProcessUtil
import org.zeroturnaround.process.Processes
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun main(args: Array<String>) {

    val task = Exec.newBuilder()
            .setCommand("bash")
            .addAllArgs(arrayListOf("-c", """trap "echo Booh!" 1 2 3 6; for i in $(seq 1 10000); do echo sleeping ${'$'}i; sleep ${'$'}i; done"""))
            .setWorkingDir("/tmp")
            .build()

    val processReference = AtomicReference<StartedProcess>()

    val idleOutputStream = OnIdleOutputStream(NullOutputStream.NULL_OUTPUT_STREAM, "idle", 3, TimeUnit.SECONDS, Runnable {
        try {
            val process = processReference.get().process
            val newStandardProcess = Processes.newStandardProcess(process, PidUtil.getPid(process))
            ProcessUtil.destroyGracefullyOrForcefullyAndWait(newStandardProcess, 30, TimeUnit.SECONDS, 30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        println("${Date()} killed!")
    })

    processReference.set(ProcessExecutor()
            .command(arrayListOf(task.command) + task.argsList)
            .directory(File(task.workingDir))

            .redirectOutputAlsoTo(idleOutputStream)
            .redirectOutputAlsoTo(System.out)
            .redirectOutputAlsoTo(Slf4jStream.of("CommandLine:STDOUT").asTrace())

            .redirectErrorAlsoTo(idleOutputStream)
            .redirectErrorAlsoTo(System.err)
            .redirectErrorAlsoTo(Slf4jStream.of("CommandLine:STDERR").asTrace())

            .destroyOnExit()
            .start())

    processReference.get().process.waitFor()
    Thread.sleep(100)
    println("Exiting!")
}

