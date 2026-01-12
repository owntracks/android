package org.owntracks.android.data.repos

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageQueueEntity::class], version = 1, exportSchema = true)
abstract class MessageQueueDatabase : RoomDatabase() {
  abstract fun messageQueueDao(): MessageQueueDao
}
