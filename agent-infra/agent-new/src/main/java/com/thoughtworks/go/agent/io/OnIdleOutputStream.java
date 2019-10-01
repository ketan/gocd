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

package com.thoughtworks.go.agent.io;

import lombok.extern.slf4j.Slf4j;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Slf4j
class OnIdleOutputStream extends FilterOutputStream {
    private final Thread monitorThread;
    private final LinkedBlockingDeque<Object> queue = new LinkedBlockingDeque<>();

    public OnIdleOutputStream(OutputStream out, String name, Long idleTimeout, TimeUnit unit, Runnable callback) {
        super(out);

        monitorThread = new Thread("OnIdleOutputStream" + name) {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (queue.pollLast(idleTimeout, unit) == null) {
                            callback.run();
                        }
                    } catch (InterruptedException e) {
                        log.warn("Thread was interrupted while polling on queue", e);
                        interrupt();
                    }
                }
            }
        };

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    public void write(int b) throws IOException {
        try {
            super.write(b);
        } finally {
            addToQueue();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        try {
            super.write(b);
        } finally {
            addToQueue();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            super.write(b, off, len);
        } finally {
            addToQueue();
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            super.flush();
        } finally {
            addToQueue();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            addToQueue();
        }
    }

    private void addToQueue() throws IOException {
        try {
            queue.putFirst(new Object());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
