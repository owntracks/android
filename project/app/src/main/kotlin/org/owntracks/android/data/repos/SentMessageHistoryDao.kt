package org.owntracks.android.data.repos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SentMessageHistoryDao {
  @Insert suspend fun insert(entity: SentMessageHistoryEntity): Long

  @Query("DELETE FROM SentMessageHistory WHERE sentAt < :cutoffTimestamp")
  suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

  @Query("DELETE FROM SentMessageHistory") suspend fun clear()

  @Query("SELECT COUNT(*) FROM SentMessageHistory") suspend fun getCount(): Int

  @Query("SELECT * FROM SentMessageHistory ORDER BY sentAt ASC")
  suspend fun getAll(): List<SentMessageHistoryEntity>
}
