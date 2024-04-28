package org.owntracks.android.services

import com.squareup.tape2.ObjectQueue
import com.squareup.tape2.QueueFile
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingDeque
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageEncrypted
import org.owntracks.android.model.messages.MessageUnknown
import org.owntracks.android.support.Parser
import timber.log.Timber

class BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(
    capacity: Int,
    path: File,
    parser: Parser
) : LinkedBlockingDeque<MessageBase>(capacity) {
  private val parallelDiskQueueHead: ObjectQueue<MessageBase>
  private val parallelDiskQueue: ObjectQueue<MessageBase>

  init {
    // We need a separate head slot to support pushing back onto the queue head, which QueueFile
    // doesn't support
    val file = path.resolve("messageQueue.dat")
    val queueFile = diskBackedQueueOrNull(file)

    val headSlotFile = path.resolve("messageQueueHead.dat")
    val headQueueFile = diskBackedQueueOrNull(headSlotFile)

    val messageBaseConverter =
        object : ObjectQueue.Converter<MessageBase> {
          override fun from(source: ByteArray): MessageBase =
              try {
                parser.fromUnencryptedJson(source)
              } catch (exception: Exception) {
                Timber.w("Unable to recover message from queue: ${source.toString(Charsets.UTF_8)}")
                MessageUnknown
              }

          override fun toStream(value: MessageBase, sink: OutputStream) {
            sink.write(parser.toUnencryptedJsonBytes(value))
          }
        }

    parallelDiskQueue =
        queueFile?.run { ObjectQueue.create(this, messageBaseConverter) }
            ?: ObjectQueue.createInMemory()

    parallelDiskQueueHead =
        headQueueFile?.run { ObjectQueue.create(this, messageBaseConverter) }
            ?: ObjectQueue.createInMemory()

    (parallelDiskQueueHead.asList() + parallelDiskQueue.asList())
        .filter { it !is MessageEncrypted }
        .forEach {
          if (!offerLast(it)) {
            Timber.w("On-disk queue contains message that won't fit into queue. Dropping: $it")
          }
        }
    resyncQueueToDisk()
  }

  private fun diskBackedQueueOrNull(file: File) =
      try {
        QueueFile.Builder(file).build()
      } catch (e: IOException) {
        Timber.e("Error initializing queue storage at $file. Re-initializing")
        file.delete()
        try {
          QueueFile.Builder(file).build()
        } catch (e: Exception) {
          null
        }
      }

  override fun offer(messageBase: MessageBase?): Boolean {
    synchronized(parallelDiskQueue) {
      val result = super.offerLast(messageBase)
      if (!result) {
        return false
      }
      try {
        messageBase?.run(parallelDiskQueue::add)
      } catch (e: IOException) {
        Timber.e(e, "Error adding message to disk Queue")
        super.removeFirst()
        return false
      }
      return true
    }
  }

  override fun poll(): MessageBase? {
    val head = super.poll()
    synchronized(parallelDiskQueue) {
      head?.run {
        if (parallelDiskQueueHead.isEmpty) {
          try {
            parallelDiskQueue.remove()
          } catch (e: Exception) {
            Timber.e(e, "Unable to remove head of diskQueue")
            resyncQueueToDisk()
          }
        } else {
          try {
            parallelDiskQueueHead.remove()
          } catch (e: Exception) {
            Timber.e(e, "Unable to remove head of diskQueue")
            resyncQueueToDisk()
          }
        }
      }
    }
    return head
  }

  private fun resyncQueueToDisk() {
    synchronized(parallelDiskQueue) {
      parallelDiskQueue.clear()
      parallelDiskQueueHead.clear()
      this.forEach { parallelDiskQueue.add(it) }
    }
  }

  override fun offerFirst(messageBase: MessageBase?): Boolean {
    return if (super.offerFirst(messageBase)) {
      messageBase?.run {
        parallelDiskQueueHead.clear()
        parallelDiskQueueHead.add(this)
      }
      true
    } else {
      false
    }
  }

  override fun take(): MessageBase {
    val head = super.take()
    synchronized(parallelDiskQueue) {
      if (parallelDiskQueueHead.isEmpty) {
        try {
          parallelDiskQueue.remove()
        } catch (e: Exception) {
          Timber.e(e, "Unable to remove head of diskQueue")
          resyncQueueToDisk()
        }
      } else {
        try {
          parallelDiskQueueHead.remove()
        } catch (e: Exception) {
          Timber.e(e, "Unable to remove head of diskQueue")
          resyncQueueToDisk()
        }
      }
    }
    return head
  }
}
