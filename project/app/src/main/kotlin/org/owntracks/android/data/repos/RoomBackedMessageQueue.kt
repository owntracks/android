package org.owntracks.android.data.repos

import android.content.Context
import androidx.room.Room
import com.squareup.tape2.ObjectQueue
import com.squareup.tape2.QueueFile
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageEncrypted
import org.owntracks.android.model.messages.MessageUnknown
import timber.log.Timber

/**
 * Room-backed message queue implementation.
 *
 * This implementation provides async persistence using Room database with an in-memory cache for
 * quick access.
 *
 * @param capacity Maximum number of messages to hold
 * @param applicationContext Android application context
 * @param parser JSON parser for serializing/deserializing messages
 * @param ioDispatcher Coroutine dispatcher for IO operations
 */
class RoomBackedMessageQueue(
    private val capacity: Int,
    private val applicationContext: Context,
    private val parser: Parser,
    private val ioDispatcher: CoroutineDispatcher
) {
  private val db: MessageQueueDatabase =
      Room.databaseBuilder(applicationContext, MessageQueueDatabase::class.java, "message_queue")
          .build()

  private val dao = db.messageQueueDao()
  private val sequenceNumberGenerator = AtomicLong(0L)
  private val mutex = Mutex()

  private val _queueSize = MutableStateFlow(0)
  val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

  /** Initializes the queue by loading persisted messages and migrating from legacy storage */
  suspend fun initialize(legacyStoragePath: File) {
    withContext(ioDispatcher) {
      mutex.withLock {
        // Initialize sequence number from database
        val maxSeq = dao.getMaxSequenceNumber() ?: 0L
        sequenceNumberGenerator.set(maxSeq)

        // Get current count
        val currentCount = dao.getCount()
        _queueSize.value = currentCount

        Timber.d("Initialized RoomBackedMessageQueue with $currentCount messages")

        // Migrate from legacy tape2 storage if exists
        migrateLegacyStorage(legacyStoragePath)
      }
    }
  }

  private suspend fun migrateLegacyStorage(legacyStoragePath: File) {
    val queueFile = legacyStoragePath.resolve("messageQueue.dat")
    val headFile = legacyStoragePath.resolve("messageQueueHead.dat")

    if (!queueFile.exists() && !headFile.exists()) {
      Timber.d("No legacy storage found, skipping migration")
      return
    }

    Timber.i("Found legacy storage files, attempting migration")
    try {
      val messagesToMigrate = mutableListOf<MessageBase>()

      // Read from legacy tape2 files
      try {
        // Read head slot first (if exists)
        if (headFile.exists()) {
          val headMessages = readLegacyQueueFile(headFile, "head slot")
          messagesToMigrate.addAll(headMessages)
        }

        // Then read main queue
        if (queueFile.exists()) {
          val queueMessages = readLegacyQueueFile(queueFile, "main queue")
          messagesToMigrate.addAll(queueMessages)
        }

        Timber.i("Successfully read ${messagesToMigrate.size} messages from legacy storage")
      } catch (e: Exception) {
        Timber.e(e, "Error reading from legacy storage, continuing without migration")
        return
      }

      // Import messages into Room database
      var migratedCount = 0
      messagesToMigrate
          .filter { it !is MessageEncrypted }
          .forEach { message ->
            try {
              enqueueInternal(message)
              migratedCount++
            } catch (e: Exception) {
              Timber.w(e, "Failed to migrate message: $message")
            }
          }

      Timber.i("Successfully migrated $migratedCount messages to Room database")

      // Delete legacy files after successful migration
      if (migratedCount > 0) {
        try {
          var deletedCount = 0
          if (queueFile.exists() && queueFile.delete()) {
            deletedCount++
            Timber.d("Deleted legacy queue file: messageQueue.dat")
          }
          if (headFile.exists() && headFile.delete()) {
            deletedCount++
            Timber.d("Deleted legacy head file: messageQueueHead.dat")
          }
          Timber.i("Successfully cleaned up $deletedCount legacy storage files")
        } catch (e: Exception) {
          Timber.w(e, "Failed to delete legacy storage files")
        }
      }
    } catch (e: Exception) {
      Timber.e(e, "Error during legacy storage migration")
    }
  }

  /**
   * Reads messages from a legacy tape2 QueueFile
   *
   * @param file The QueueFile to read from
   * @param description Human-readable description for logging
   * @return List of messages read from the file
   */
  private fun readLegacyQueueFile(file: File, description: String): List<MessageBase> {
    val messages = mutableListOf<MessageBase>()

    try {
      val queueFile = QueueFile.Builder(file).build()

      val messageConverter =
          object : ObjectQueue.Converter<MessageBase> {
            override fun from(source: ByteArray): MessageBase =
                try {
                  parser.fromUnencryptedJson(source)
                } catch (exception: Exception) {
                  Timber.w(
                      "Unable to recover message from $description: ${source.toString(Charsets.UTF_8)}")
                  MessageUnknown
                }

            override fun toStream(value: MessageBase, sink: OutputStream) {
              sink.write(parser.toUnencryptedJsonBytes(value))
            }
          }

      val objectQueue = ObjectQueue.create(queueFile, messageConverter)
      messages.addAll(objectQueue.asList())
      Timber.d("Read ${messages.size} messages from legacy $description")
    } catch (e: IOException) {
      Timber.e(e, "Error reading legacy $description at $file")
      // Try to delete corrupted file
      try {
        file.delete()
        Timber.w("Deleted corrupted legacy file: ${file.name}")
      } catch (deleteError: Exception) {
        Timber.w(deleteError, "Failed to delete corrupted file: ${file.name}")
      }
    }

    return messages
  }

  /**
   * Adds a message to the end of the queue.
   *
   * @param message The message to enqueue
   * @return true if the message was added, false if the queue is full
   */
  suspend fun enqueue(message: MessageBase): Boolean {
    return withContext(ioDispatcher) {
      mutex.withLock {
        val currentSize = dao.getCount()
        if (currentSize >= capacity) {
          Timber.w("Queue at capacity ($capacity), cannot enqueue message")
          return@withContext false
        }

        enqueueInternal(message)
        true
      }
    }
  }

  private suspend fun enqueueInternal(message: MessageBase) {
    try {
      val messageJson = parser.toJsonPlain(message)
      val sequenceNumber = sequenceNumberGenerator.incrementAndGet()
      val entity =
          MessageQueueEntity(
              sequenceNumber = sequenceNumber, messageJson = messageJson, isHeadSlot = false)

      dao.insert(entity)
      _queueSize.value = dao.getCount()
      Timber.d("Enqueued message with sequence $sequenceNumber")
    } catch (e: Exception) {
      Timber.e(e, "Error enqueuing message to database")
      throw e
    }
  }

  /**
   * Adds a message to the head of the queue (for re-queueing failed messages).
   *
   * @param message The message to add to the head
   * @return true if the message was added, false if the queue is full
   */
  suspend fun requeue(message: MessageBase): Boolean {
    return withContext(ioDispatcher) {
      mutex.withLock {
        val currentSize = dao.getCount()
        if (currentSize >= capacity) {
          Timber.w("Queue at capacity ($capacity), cannot requeue message")
          return@withContext false
        }

        try {
          val messageJson = parser.toJsonPlain(message)
          val sequenceNumber = sequenceNumberGenerator.incrementAndGet()
          val entity =
              MessageQueueEntity(
                  sequenceNumber = sequenceNumber, messageJson = messageJson, isHeadSlot = true)

          dao.insert(entity)
          _queueSize.value = dao.getCount()
          Timber.d("Requeued message to head with sequence $sequenceNumber")
          true
        } catch (e: Exception) {
          Timber.e(e, "Error requeuing message to database")
          false
        }
      }
    }
  }

  /**
   * Retrieves and removes the next message from the queue. Head slot messages are prioritized.
   *
   * @return The next message, or null if the queue is empty
   */
  suspend fun dequeue(): MessageBase? {
    return withContext(ioDispatcher) {
      mutex.withLock {
        try {
          // Check head slot first
          val headMessage = dao.getNextHeadSlotMessage()
          val entity = headMessage ?: dao.getNextRegularMessage()

          if (entity != null) {
            val message =
                try {
                  parser.fromUnencryptedJson(entity.messageJson.toByteArray())
                } catch (e: Exception) {
                  Timber.w(e, "Unable to deserialize message from queue: ${entity.messageJson}")
                  MessageUnknown
                }

            // Remove from database
            dao.deleteById(entity.id)
            _queueSize.value = dao.getCount()

            Timber.d("Dequeued message with id ${entity.id}")
            return@withLock message
          }

          return@withLock null
        } catch (e: Exception) {
          Timber.e(e, "Error dequeuing message from database")
          null
        }
      }
    }
  }

  /** Returns the current size of the queue */
  suspend fun size(): Int {
    return withContext(ioDispatcher) { dao.getCount() }
  }

  /** Clears all messages from the queue */
  suspend fun clear() {
    withContext(ioDispatcher) {
      mutex.withLock {
        try {
          dao.clear()
          _queueSize.value = 0
          Timber.d("Cleared message queue")
        } catch (e: Exception) {
          Timber.e(e, "Error clearing message queue")
        }
      }
    }
  }

  /** Closes the database connection */
  fun close() {
    db.close()
  }
}
