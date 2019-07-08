package com.thoughtworks.go.agent.io

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.zeroturnaround.exec.stream.NullOutputStream
import java.util.concurrent.TimeUnit

internal class OnIdleOutputStreamTest {

    @Test
    fun `should not invoke callback before timeout`() {
        val callback = mock(Runnable::class.java)
        OnIdleOutputStream(NullOutputStream(), "foo", 10, TimeUnit.SECONDS, callback).use {
            it.write(10)
            it.close()
        }
        verify(callback, never()).run()
    }

    @Test
    fun `should invoke callback on timeout if write is not done before timeout`() {
        val callback = mock(Runnable::class.java)
        // create a stream with 2 second idle timeout
        OnIdleOutputStream(NullOutputStream(), "foo", 1, TimeUnit.SECONDS, callback).use {
            // timeouts should not be called, if the interval between each invocation is <2s
            Thread.sleep(100)
            verify(callback, never()).run()

            it.write(10)
            verify(callback, never()).run()

            it.write(10)
            Thread.sleep(500)
            verify(callback, never()).run()

            it.write(10)
            Thread.sleep(500)
            verify(callback, never()).run()

            it.write(10)
            // timeout should happen here
            Thread.sleep(1100)
            verify(callback, times(1)).run()

            it.write(10)
            Thread.sleep(10)
            // no new timeouts
            verify(callback, times(1)).run()

            it.write(10)
            // one new timeout
            Thread.sleep(1100)
            verify(callback, times(2)).run()
        }
    }
}
