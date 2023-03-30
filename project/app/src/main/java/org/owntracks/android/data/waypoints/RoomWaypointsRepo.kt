package org.owntracks.android.data.waypoints

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.owntracks.android.di.CoroutineScopes

@Singleton
class RoomWaypointsRepo @Inject constructor(
    @ApplicationContext applicationContext: Context,
    @CoroutineScopes.IoDispatcher val ioDispatcher: CoroutineDispatcher
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
