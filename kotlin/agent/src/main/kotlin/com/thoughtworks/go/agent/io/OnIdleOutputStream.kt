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

package com.thoughtworks.go.agent.io

import org.kxtra.slf4j.getLogger
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class OnIdleOutputStream(out: OutputStream, private val name: String, private val idleTimeout: Long, private val unit: TimeUnit, private val callback: Runnable) : FilterOutputStream(out) {

    companion object {
        val logger = getLogger()
    }

    private val monitorThread: Thread
    private val queue = LinkedBlockingDeque<Any>()

    init {
        monitorThread = object : Thread("OnIdleOutputStream-${name}") {
            override fun run() {
                while (true) {
                    try {
                        if (queue.pollLast(idleTimeout, unit) == null) {
                            callback.run()
                        }
                    } catch (e: InterruptedException) {
                        logger.warn("Thread was interrupted while polling on queue", e)
                        interrupt()
                    }
                }
            }
        }
        monitorThread.isDaemon = true
        monitorThread.start()
    }

    override fun write(b: Int) {
        try {
            super.write(b)
        } finally {
            queue.putFirst(Object())
        }
    }

    override fun write(b: ByteArray) {
        try {
            super.write(b)
        } finally {
            queue.putFirst(Object())
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        try {
            super.write(b, off, len)
        } finally {
            queue.putFirst(Object())
        }
    }

    override fun flush() {
        try {
            super.flush()
        } finally {
            queue.putFirst(Object())
        }
    }

    override fun close() {
        try {
            super.close()
        } finally {
            queue.putFirst(Object())
        }
    }

}
