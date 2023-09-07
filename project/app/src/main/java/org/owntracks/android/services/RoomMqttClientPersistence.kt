package org.owntracks.android.services

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.nio.ByteBuffer
import java.util.Enumeration
import org.eclipse.paho.client.mqttv3.MqttClientPersistence
import org.eclipse.paho.client.mqttv3.MqttPersistable
import org.eclipse.paho.client.mqttv3.MqttPersistenceException
import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData

/**
 * Implementation of [MqttClientPersistence] that stores data in a Room database
 */
class RoomMqttClientPersistence(applicationContext: Context, forTesting: Boolean = false) :
    MqttClientPersistence {

    private val db = if (forTesting) {
        Room.inMemoryDatabaseBuilder(
            applicationContext,
            MqttPersistableDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    } else {
        Room.databaseBuilder(
            applicationContext,
            MqttPersistableDatabase::class.java,
            "pahoMqttPersistence"
        )
            .build()
    }

    @Entity
    data class MqttPersistableForClient(
        @PrimaryKey val clientId: String,
        @ColumnInfo(name = "persistable", typeAffinity = ColumnInfo.BLOB) val persistable: ByteArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MqttPersistableForClient

            if (clientId != other.clientId) return false
            if (!persistable.contentEquals(other.persistable)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = clientId.hashCode()
            result = 31 * result + persistable.contentHashCode()
            return result
        }
    }

    @Dao
    interface MqttPersistableDao {
        @Query("SELECT * FROM MqttPersistableForClient WHERE clientId = :clientId")
        fun getById(clientId: String): MqttPersistableForClient?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(mqttPersistable: MqttPersistableForClient)

        @Query("SELECT clientId FROM MqttPersistableForClient")
        fun keys(): List<String>

        @Delete
        fun delete(mqttPersistable: MqttPersistableForClient)

        @Query("DELETE FROM MqttPersistableForClient")
        fun deleteAll()
    }

    @Database(entities = [MqttPersistableForClient::class], version = 1)
    abstract class MqttPersistableDatabase : RoomDatabase() {
        abstract fun mqttPersistableDao(): MqttPersistableDao
    }

    override fun close() {
        // NOOP
    }

    override fun open(clientId: String?, serverURI: String?) {
        // NOOP
    }

    fun MqttPersistable.toByteArray(): ByteArray {
        return ByteArray(8 + headerLength + payloadLength).also {
            ByteBuffer.wrap(it)
                .apply {
                    putInt(headerLength)
                    put(headerBytes, headerOffset, headerLength)
                    putInt(payloadLength)
                    payloadBytes?.run { this@apply.put(payloadBytes, payloadOffset, payloadLength) }
                }
        }
    }

    private fun ByteArray.toMqttPersistentData(key: String) =
        ByteBuffer.wrap(this)
            .run {
                val headerLength = int
                val headerBytes = ByteArray(headerLength)
                get(headerBytes)
                val payloadLength = int
                val payloadBytes = ByteArray(payloadLength)
                get(payloadBytes)
                MqttPersistentData(key, headerBytes, 0, headerLength, payloadBytes, 0, payloadLength)
            }

    override fun put(key: String, persistable: MqttPersistable) {
        db.mqttPersistableDao()
            .insert(MqttPersistableForClient(key, persistable.toByteArray()))
    }

    override fun get(key: String): MqttPersistable {
        return db.mqttPersistableDao()
            .getById(key)?.persistable?.toMqttPersistentData(key) ?: throw MqttPersistenceException()
    }

    override fun remove(key: String) {
        db.mqttPersistableDao()
            .delete(MqttPersistableForClient(key, null))
    }

    override fun keys(): Enumeration<*> = db.mqttPersistableDao()
        .keys()
        .asSequence()
        .toEnumeration()

    override fun clear() {
        db.mqttPersistableDao()
            .deleteAll()
    }

    override fun containsKey(key: String): Boolean =
        db.mqttPersistableDao()
            .getById(key) != null

    private fun <T> Sequence<T>.toEnumeration(): Enumeration<T> {
        val iterator = this.iterator()
        return object : Enumeration<T> {
            override fun hasMoreElements() = iterator.hasNext()
            override fun nextElement(): T = iterator.next()
        }
    }
}
