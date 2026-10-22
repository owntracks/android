package org.owntracks.android.location.profiles

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowFallbackTest {
  @Test
  fun `the fallback is used if nothing is emitted in time`() = runTest {
    val source = MutableSharedFlow<String>()
    val values = mutableListOf<String>()
    val job = launch { source.orFallbackAfter(3.seconds, "fallback").toList(values) }
    advanceTimeBy(3.1.seconds)
    source.emit("real")
    advanceTimeBy(1.seconds)
    job.cancel()
    assertEquals(listOf("fallback", "real"), values)
  }

  @Test
  fun `the fallback isn't used once something has been emitted`() = runTest {
    val source = MutableSharedFlow<String>()
    val values = mutableListOf<String>()
    val job = launch { source.orFallbackAfter(3.seconds, "fallback").toList(values) }
    advanceTimeBy(1.seconds)
    source.emit("real")
    advanceTimeBy(5.seconds)
    job.cancel()
    assertEquals(listOf("real"), values)
  }
}
