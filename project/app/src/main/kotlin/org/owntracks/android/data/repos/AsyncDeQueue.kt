package org.owntracks.android.data.repos

import java.io.File
import kotlinx.coroutines.flow.StateFlow
import org.owntracks.android.model.messages.MessageBase

interface AsyncDeQueue {
  val queueSize: StateFlow<Int>

  suspend fun initialize(legacyStoragePath: File)

  suspend fun enqueue(message: MessageBase): Boolean

  suspend fun requeue(message: MessageBase): Boolean

  suspend fun dequeue(): MessageBase?

  suspend fun awaitMessage(): MessageBase

  fun signalMessageAvailable()

  suspend fun size(): Int

  suspend fun clear()

  fun close()
}
