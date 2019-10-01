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

import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.stream.NullOutputStream;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

class OnIdleOutputStreamTest {

    @Test
    void shouldNotInvokeCallbackBeforeTimeout() throws IOException {
        Runnable callback = mock(Runnable.class);
        OnIdleOutputStream os = new OnIdleOutputStream(new NullOutputStream(), "foo", 10L, TimeUnit.SECONDS, callback);
        os.write(10);
        os.close();
        verify(callback, never()).run();
    }

    @Test
    void shouldInvokeCallbackOnTimeoutIfWriteIsNotDoneBeforeTimeout() throws InterruptedException, IOException {
        Runnable callback = mock(Runnable.class);
        // create a stream with 2 second idle timeout
        OnIdleOutputStream os = new OnIdleOutputStream(new NullOutputStream(), "foo", 1L, TimeUnit.SECONDS, callback);
        // timeouts should not be called, if the interval between each invocation is <2s
        Thread.sleep(100);
        verify(callback, never()).run();

        os.write(10);
        verify(callback, never()).run();

        os.write(10);
        Thread.sleep(500);
        verify(callback, never()).run();

        os.write(10);
        Thread.sleep(500);
        verify(callback, never()).run();

        os.write(10);
        // timeout should happen here
        Thread.sleep(1100);
        verify(callback, times(1)).run();

        os.write(10);
        Thread.sleep(10);
        // no new timeouts
        verify(callback, times(1)).run();

        os.write(10);
        // one new timeout
        Thread.sleep(1100);
        verify(callback, times(2)).run();
    }
}
