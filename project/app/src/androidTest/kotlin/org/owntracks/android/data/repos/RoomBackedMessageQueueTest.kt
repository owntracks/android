package org.owntracks.android.data.repos

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageLocation

@RunWith(AndroidJUnit4::class)
class RoomBackedMessageQueueTest {
  private lateinit var queue: RoomBackedMessageQueue
  private val parser = Parser(null)
  private val random = Random(1)

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    queue = RoomBackedMessageQueue(100, context, parser, Dispatchers.IO)
  }

  @After
  fun teardown() {
    queue.close()
  }

  private fun generateRandomMessageLocation(): MessageLocation {
    return MessageLocation().apply {
      longitude = random.nextDouble()
      latitude = random.nextDouble()
      accuracy = random.nextInt()
    }
  }

  @Test
  fun givenAnEmptyQueueWhenDequeuingThenNullIsReturned() = runBlocking {
    queue.initialize(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir)
    val result = queue.dequeue()
    assertNull(result)
  }

  @Test
  fun givenAnEmptyQueueWhenAddingAnItemTheQueueSizeIs1() = runBlocking {
    queue.initialize(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir)
    val message = generateRandomMessageLocation()
    val added = queue.enqueue(message)
    assertTrue(added)
    assertEquals(1, queue.size())
  }

  @Test
  fun givenHeadSlotAndRegularMessagesWhenDequeuingThenHeadSlotIsPrioritized() = runBlocking {
    queue.initialize(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir)
    val regularMessage1 = generateRandomMessageLocation()
    val regularMessage2 = generateRandomMessageLocation()
    queue.enqueue(regularMessage1)
    queue.enqueue(regularMessage2)
    val headMessage = generateRandomMessageLocation()
    queue.requeue(headMessage)
    assertEquals(3, queue.size())
    val first = queue.dequeue() as MessageLocation
    assertEquals(headMessage.longitude, first.longitude, 0.0001)
    val second = queue.dequeue() as MessageLocation
    assertEquals(regularMessage1.longitude, second.longitude, 0.0001)
    val third = queue.dequeue() as MessageLocation
    assertEquals(regularMessage2.longitude, third.longitude, 0.0001)
    assertNull(queue.dequeue())
  }
}
