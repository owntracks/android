package org.owntracks.android.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogRingBufferTest {
    @Test
    fun `Given an empty buffer, when adding an entry, then the buffer has one entry`() {
        val logEntry = LogEntry(3, "tag", "message", "main")
        val logRingBuffer = LogRingBuffer(5)
        logRingBuffer.add(logEntry)
        assertEquals(1, logRingBuffer.all().size)
        assertEquals(logEntry, logRingBuffer.all().first())
    }

    @Test
    fun `Given a full buffer, when adding a new log entry, then the buffer does not contain the first entry`() {
        val logEntries = (0..5).map { LogEntry(it, "tag", "message", "main") }
        val logRingBuffer = LogRingBuffer(5)
        logEntries.forEach { logRingBuffer.add(it) }
        assertEquals(5, logRingBuffer.all().size)
        assertFalse(logRingBuffer.all().contains(logEntries.first()))
    }

    @Test
    fun `Given a full buffer, when requesting the logs, they are returned in the correct order`() {
        val logEntries = (0..12).map { LogEntry(it, "tag", "message", "main") }
        val logRingBuffer = LogRingBuffer(5)
        logEntries.forEach { logRingBuffer.add(it) }
        assertEquals(8, logRingBuffer.all().first().priority)
        assertEquals(12, logRingBuffer.all().last().priority)
    }

    @Test
    fun `Given a partial buffer, when requesting the logs, they are returned in the correct order`() {
        val logEntries = (0..9).map { LogEntry(it, "tag", "message", "main") }
        val logRingBuffer = LogRingBuffer(20)
        logEntries.forEach { logRingBuffer.add(it) }
        assertEquals(0, logRingBuffer.all().first().priority)
        assertEquals(9, logRingBuffer.all().last().priority)
    }

    @Test
    fun `Given a full buffer, when clearing, then the buffer is empty`() {
        val logEntries = (0..5).map { LogEntry(it, "tag", "message", "main") }
        val logRingBuffer = LogRingBuffer(5)
        logEntries.forEach { logRingBuffer.add(it) }
        logRingBuffer.clear()
        assert(logRingBuffer.all().isEmpty())
    }
}
