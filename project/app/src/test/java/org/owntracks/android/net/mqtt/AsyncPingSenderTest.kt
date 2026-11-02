package org.owntracks.android.net.mqtt

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncPingSenderTest {
  private val testScope = TestScope()
  private val sender = AsyncPingSender(testScope)

  private fun advance(milliseconds: Long) {
    testScope.advanceTimeBy(milliseconds)
    testScope.runCurrent()
  }

  @Test
  fun `a new schedule replaces the pending one rather than adding a second chain`() {
    val comms: ClientComms = mock {}
    sender.init(comms)

    sender.schedule(1000)
    sender.schedule(1000)
    advance(1000)

    verify(comms, times(1)).checkForActivity()
  }

  @Test
  fun `the chain survives a check scheduling the next ping from inside the running job`() {
    // Paho's checkForActivity ends by calling schedule, which cancels the job it is running in.
    val comms: ClientComms = mock {
      on { checkForActivity() } doAnswer
          {
            sender.schedule(1000)
            null
          }
    }
    sender.init(comms)

    sender.schedule(1000)
    advance(3000)

    verify(comms, times(3)).checkForActivity()
  }
}
