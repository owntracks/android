package org.owntracks.android.services

import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageLocation

class BlockingDequeueThatAlsoSometimesPersistsThingsToDiskMaybeTest {
  private val parser = Parser(null)
  private val random = Random(1)

  private fun generateRandomMessageLocation(): MessageLocation {
    return MessageLocation().apply {
      longitude = random.nextDouble()
      latitude = random.nextDouble()
      accuracy = random.nextInt()
    }
  }

  @Test
  fun `given an empty queue when polling then null is returned`() {
    val queue =
        BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(
            10, Files.createTempDirectory("").toFile(), parser)
    assertNull(queue.poll())
  }

  @Test
  fun `given an empty queue when adding an item the queue size is 1`() {
    val queue =
        BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(
            10, Files.createTempDirectory("").toFile(), parser)
    queue.offer(generateRandomMessageLocation())
    assertEquals(1, queue.size)
  }

  @Test
  fun `given a non-empty queue, when pushing an item to the head then that same item is returned on poll`() {
    val queue =
        BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(
            10, Files.createTempDirectory("").toFile(), parser)
    repeat(5) { queue.offer(generateRandomMessageLocation()) }

    val headItem = generateRandomMessageLocation()
    queue.addFirst(headItem)

    assertEquals(6, queue.size)

    val retrieved = queue.poll()
    assertEquals(headItem, retrieved)
  }

  @Test
  fun `given a file path, when initializing the queue the size is correct`() {
    val dir = Files.createTempDirectory("").toFile()
    val queue = BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(10, dir, parser)
    repeat(5) { queue.offer(generateRandomMessageLocation()) }

    val newQueue = BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(10, dir, parser)

    assertEquals(5, newQueue.size)
  }

  @Test
  fun `given a file path where the head slot is occupied, when initializing the queue the size is correct`() {
    val dir = Files.createTempDirectory("").toFile()
    val queue = BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(10, dir, parser)
    repeat(5) { queue.offer(generateRandomMessageLocation()) }

    val headItem = generateRandomMessageLocation()
    queue.addFirst(headItem)

    val newQueue = BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(10, dir, parser)

    assertEquals(6, newQueue.size)

    val retrieved = queue.poll()
    assertEquals(headItem, retrieved)
  }

  @Test
  fun `given a non-empty queue, when taking an item to the head then the item is returned`() {
    val queue =
        BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(
            10, Files.createTempDirectory("").toFile(), parser)
    repeat(5) { queue.offer(generateRandomMessageLocation()) }

    val headItem = generateRandomMessageLocation()
    queue.addFirst(headItem)

    assertEquals(6, queue.size)

    val retrieved = queue.take()
    assertEquals(headItem, retrieved)
  }

  @Test
  fun `given a corrupt file, when initializing the queue then an empty queue is created`() {
    val dir = Files.createTempDirectory("").toFile()
    dir.resolve("messageQueue.dat").writeBytes(random.nextBytes(100))
    val queue = BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(10, dir, parser)
    assertEquals(0, queue.size)
  }

  @Test
  fun `given an un-writable location, when initializing a queue then an in-memory empty queue is created`() {
    val queue = BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(10, File("/"), parser)
    assertEquals(0, queue.size)
  }
}
