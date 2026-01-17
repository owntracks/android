package org.owntracks.android.data.repos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "MessageQueue",
    indices = [Index(value = ["sequenceNumber"]), Index(value = ["isHeadSlot"])])
data class MessageQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sequenceNumber") val sequenceNumber: Long,
    @ColumnInfo(name = "messageJson") val messageJson: String,
    @ColumnInfo(name = "isHeadSlot") val isHeadSlot: Boolean = false,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
