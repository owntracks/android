package org.owntracks.android.data.repos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MessageQueueDao {
  @Insert suspend fun insert(entity: MessageQueueEntity): Long

  @Query(
      """
    SELECT * FROM MessageQueue
    WHERE isHeadSlot = 1
    ORDER BY sequenceNumber ASC
    LIMIT 1
  """)
  suspend fun getNextHeadSlotMessage(): MessageQueueEntity?

  @Query(
      """
    SELECT * FROM MessageQueue
    WHERE isHeadSlot = 0
    ORDER BY sequenceNumber ASC
    LIMIT 1
  """)
  suspend fun getNextRegularMessage(): MessageQueueEntity?

  @Query("DELETE FROM MessageQueue WHERE id = :id") suspend fun deleteById(id: Long)

  @Query("SELECT COUNT(*) FROM MessageQueue") suspend fun getCount(): Int

  @Query("DELETE FROM MessageQueue") suspend fun clear()

  @Query("SELECT * FROM MessageQueue ORDER BY sequenceNumber ASC")
  suspend fun getAll(): List<MessageQueueEntity>

  @Query("SELECT MAX(sequenceNumber) FROM MessageQueue") suspend fun getMaxSequenceNumber(): Long?

  @Transaction
  @Query("DELETE FROM MessageQueue WHERE isHeadSlot = 0")
  suspend fun clearRegularQueue()
}
