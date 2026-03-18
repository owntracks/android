package org.owntracks.android.data.repos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "SentMessageHistory",
    indices = [Index(value = ["sentAt"])])
data class SentMessageHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "messageJson") val messageJson: String,
    @ColumnInfo(name = "sentAt") val sentAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "createdAt") val createdAt: Long
)
