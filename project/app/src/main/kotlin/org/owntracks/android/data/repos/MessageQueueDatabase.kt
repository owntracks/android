package org.owntracks.android.data.repos

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MessageQueueEntity::class], version = 2, exportSchema = true)
abstract class MessageQueueDatabase : RoomDatabase() {
  abstract fun messageQueueDao(): MessageQueueDao

  companion object {
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE MessageQueue ADD COLUMN topic TEXT NOT NULL DEFAULT ''")
          }
        }
  }
}
