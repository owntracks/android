package org.owntracks.android.data.repos

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SentMessageHistoryEntity::class], version = 1, exportSchema = true)
abstract class SentMessageHistoryDatabase : RoomDatabase() {
  abstract fun sentMessageHistoryDao(): SentMessageHistoryDao
}
