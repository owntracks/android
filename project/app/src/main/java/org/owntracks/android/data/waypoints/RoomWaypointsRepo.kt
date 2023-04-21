package org.owntracks.android.data.waypoints

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.growse.lmdb_kt.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import timber.log.Timber

@Singleton
class RoomWaypointsRepo @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : WaypointsRepo() {
    @Dao
    interface WaypointDao {
        @Query("SELECT * FROM WaypointModel WHERE id = :id")
        fun get(id: Long): WaypointModel?

        @Query("SELECT * FROM WaypointModel WHERE tst = :tst")
        fun getByTst(tst: Instant): WaypointModel?

        @Query("SELECT * FROM WaypointModel")
        fun allLive(): LiveData<List<WaypointModel>>

        @Query("SELECT * FROM WaypointModel")
        fun all(): List<WaypointModel>

        @Upsert
        fun upsert(waypointModel: WaypointModel)

        @Delete
        fun delete(waypointModel: WaypointModel)

        @Insert
        fun insertAll(waypoints: List<WaypointModel>)

        @Query("SELECT COUNT(*) FROM WaypointModel")
        fun getRowCount(): Int
    }

    @Database(entities = [WaypointModel::class], version = 1)
    @TypeConverters(LocalDateTimeConverter::class)
    abstract class WaypointDatabase : RoomDatabase() {
        abstract fun waypointDao(): WaypointDao
    }

    private val db = Room.databaseBuilder(
        applicationContext,
        WaypointDatabase::class.java,
        "waypoints"
    )
        .build()

    override suspend fun getByTst(instant: Instant): WaypointModel? = db.waypointDao()
        .getByTst(instant)

    override suspend fun get(id: Long): WaypointModel? = withContext(ioDispatcher) {
        db.waypointDao()
            .get(id)
    }

    override val all: List<WaypointModel>
        get() = db.waypointDao()
            .all()

    override val allLive: LiveData<List<WaypointModel>>
        get() = db.waypointDao()
            .allLive()

    override suspend fun insertImpl(waypointModel: WaypointModel) = withContext(ioDispatcher) {
        db.waypointDao()
            .upsert(waypointModel)
    }

    override suspend fun updateImpl(waypointModel: WaypointModel) = withContext(ioDispatcher) {
        db.waypointDao()
            .upsert(waypointModel)
    }

    override suspend fun deleteImpl(waypointModel: WaypointModel) = withContext(ioDispatcher) {
        db.waypointDao()
            .delete(waypointModel)
    }

    private val _migrationCompleteFlow = MutableStateFlow(false)
    override val migrationCompleteFlow: StateFlow<Boolean> = _migrationCompleteFlow

    fun migrateFromLegacyStorage(): Job {
        return scope.launch(ioDispatcher) {
            try {
                Environment(
                    applicationContext.filesDir.resolve("objectbox/objectbox")
                        .toString(),
                    readOnly = true,
                    locking = false
                ).use {
                    it.beginTransaction()
                        .dump()
                        .filter { entry ->
                            entry.key.bytes.slice(0..3) ==
                                listOf<Byte>(0x18, 0, 0, 0x4)
                        }
                        .map { lmdbEntry ->
                            deserializeWaypointModel(lmdbEntry.value)
                        }
                        .toList()
                        .run {
                            db.waypointDao()
                                .insertAll(this)
                                .also {
                                    Timber.tag("TOOT")
                                        .i(
                                            "Waypoints Migration complete. Tried to insert ${this.size}, ended up with ${
                                                db.waypointDao()
                                                    .getRowCount()
                                            } waypoints in the repo"
                                        )
                                }
                        }
                }
            } catch (throwable: Throwable) {
                Timber.tag("TOOT")
                    .e(throwable, "Error importing waypoints from legacy storage")
            } finally {
                _migrationCompleteFlow.compareAndSet(false, true)
            }
        }
    }

    private fun deserializeWaypointModel(value: ByteArray): WaypointModel {
        ByteBuffer.wrap(value)
            .run {
                order(ByteOrder.LITTLE_ENDIAN)
                val id = 0L
                position(0x28)
                val longitude = double
                val latitude = double
                val lastTransition = int
                val radius = int
                val tst = long
                position(0x54)
                val descriptionLength = int
                val description = ByteArray(descriptionLength)
                get(description)
                return WaypointModel(
                    id,
                    String(description),
                    latitude,
                    longitude,
                    radius,
                    lastTransition = lastTransition,
                    tst = Instant.ofEpochSecond(tst)
                )
            }
    }

    /**
     * A converter that tells Room how to store [java.time.Instant]
     *
     * @constructor Create empty Local date time converter
     */
    class LocalDateTimeConverter {
        @TypeConverter
        fun toInstant(epochSeconds: Long): Instant = Instant.ofEpochSecond(epochSeconds)

        @TypeConverter
        fun toEpochSeconds(instant: Instant): Long = instant.epochSecond
    }
}
