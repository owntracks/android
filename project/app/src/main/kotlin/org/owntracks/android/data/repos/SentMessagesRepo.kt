package org.owntracks.android.data.repos

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.owntracks.android.di.CoroutineScopes.IoDispatcher
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.preferences.Preferences
import timber.log.Timber

@Singleton
class SentMessagesRepo
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    private val parser: Parser,
    private val preferences: Preferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private val db: SentMessageHistoryDatabase =
      Room.databaseBuilder(
              applicationContext, SentMessageHistoryDatabase::class.java, "sent_message_history")
          .build()

  private val dao = db.sentMessageHistoryDao()

  /** Archives a successfully sent message to history. */
  suspend fun archiveMessage(message: MessageBase) {
    if (preferences.sentDataRetentionHours == 0 && preferences.dataRetentionHours == 0) {
      // Both set to "forever" means keep everything - still archive
    }
    withContext(ioDispatcher) {
      try {
        val messageJson = parser.toJsonPlain(message)
        val entity =
            SentMessageHistoryEntity(
                messageJson = messageJson, createdAt = System.currentTimeMillis())
        dao.insert(entity)
        Timber.d("Archived sent message to history")
        cleanupIfNeeded()
      } catch (e: Exception) {
        Timber.e(e, "Error archiving sent message")
      }
    }
  }

  /** Runs cleanup based on current retention preferences. */
  suspend fun cleanupIfNeeded() {
    withContext(ioDispatcher) {
      try {
        val sentRetentionHours = preferences.sentDataRetentionHours
        val dataRetentionHours = preferences.dataRetentionHours

        // Use the more restrictive (smaller non-zero) retention
        val effectiveRetentionHours =
            when {
              sentRetentionHours > 0 && dataRetentionHours > 0 ->
                  minOf(sentRetentionHours, dataRetentionHours)
              sentRetentionHours > 0 -> sentRetentionHours
              dataRetentionHours > 0 -> dataRetentionHours
              else -> 0 // Both 0 = forever
            }

        if (effectiveRetentionHours > 0) {
          val cutoff =
              System.currentTimeMillis() - effectiveRetentionHours.toLong() * 3600 * 1000
          val deleted = dao.deleteOlderThan(cutoff)
          if (deleted > 0) {
            Timber.i("Cleaned up $deleted expired sent messages")
          }
        }
      } catch (e: Exception) {
        Timber.e(e, "Error during sent message cleanup")
      }
    }
  }

  /** Deletes all sent/synchronized messages. */
  suspend fun clearAll() {
    withContext(ioDispatcher) {
      try {
        dao.clear()
        Timber.i("Cleared all sent message history")
      } catch (e: Exception) {
        Timber.e(e, "Error clearing sent message history")
      }
    }
  }

  /** Returns the count of archived sent messages. */
  suspend fun getCount(): Int {
    return withContext(ioDispatcher) { dao.getCount() }
  }

  /** Returns all archived messages deserialized back to MessageBase objects. */
  suspend fun getAllMessages(): List<MessageBase> {
    return withContext(ioDispatcher) {
      dao.getAll().mapNotNull { entity ->
        try {
          parser.fromUnencryptedJson(entity.messageJson.toByteArray())
        } catch (e: Exception) {
          Timber.w(e, "Unable to deserialize archived message: ${entity.messageJson}")
          null
        }
      }
    }
  }
}
