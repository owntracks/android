package org.owntracks.android.data.waypoints

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.google.flatbuffers.Constants
import com.google.flatbuffers.FlatBufferBuilder
import com.google.flatbuffers.Table
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude

@Singleton
class RoomWaypointsRepo
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : WaypointsRepo(scope,applicationContext,ioDispatcher) {
  @Dao
  interface WaypointDao {
    @Query("SELECT * FROM WaypointModel WHERE id = :id") fun get(id: Long): WaypointModel?

    @Query("SELECT * FROM WaypointModel WHERE tst = :tst")
    fun getByTst(tst: Instant): WaypointModel?

    @Query("SELECT * FROM WaypointModel") fun allLive(): Flow<List<WaypointModel>>

    @Query("SELECT * FROM WaypointModel") fun all(): List<WaypointModel>

    @Query("DELETE FROM WaypointModel") fun deleteAll()

    @Upsert fun upsert(waypointModel: WaypointModel)

    @Delete fun delete(waypointModel: WaypointModel)

    @Transaction // Add this to ensure atomic operation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(waypoints: List<WaypointModel>)

    @Query("SELECT COUNT(*) FROM WaypointModel") fun getRowCount(): Int
  }

  @Database(entities = [WaypointModel::class], version = 1)
  @TypeConverters(
      LocalDateTimeConverter::class, LatitudeTypeConverter::class, LongitudeTypeConverter::class)
  abstract class WaypointDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
  }

  private val db =
      Room.databaseBuilder(applicationContext, WaypointDatabase::class.java, "waypoints").build()

  override suspend fun getByTst(instant: Instant): WaypointModel? =
      db.waypointDao().getByTst(instant)

  override suspend fun get(id: Long): WaypointModel? =
      withContext(ioDispatcher) { db.waypointDao().get(id) }

  override val all: List<WaypointModel>
    get() = db.waypointDao().all()

  override val allLive: Flow<List<WaypointModel>>
    get() = db.waypointDao().allLive()

  override suspend fun clearImpl() {
    db.waypointDao().deleteAll()
  }

  override suspend fun insertImpl(waypointModel: WaypointModel) =
      withContext(ioDispatcher) { db.waypointDao().upsert(waypointModel) }

  override suspend fun insertAllImpl(waypoints: List<WaypointModel>) =
      withContext(ioDispatcher) { db.waypointDao().insertAll(waypoints) }

  override suspend fun updateImpl(waypointModel: WaypointModel) =
      withContext(ioDispatcher) { db.waypointDao().upsert(waypointModel) }

  override suspend fun deleteImpl(waypointModel: WaypointModel) =
      withContext(ioDispatcher) { db.waypointDao().delete(waypointModel) }

  private val _migrationCompleteFlow = MutableStateFlow(false)
  override val migrationCompleteFlow: StateFlow<Boolean> = _migrationCompleteFlow


  /**
   * A converter that tells Room how to store [java.time.Instant]
   *
   * @constructor Create empty Local date time converter
   */
  class LocalDateTimeConverter {
    @TypeConverter
    fun toInstant(epochSeconds: Long?): Instant? = epochSeconds?.run(Instant::ofEpochSecond)

    @TypeConverter fun toEpochSeconds(instant: Instant?): Long? = instant?.epochSecond
  }

  class LatitudeTypeConverter {
    @TypeConverter fun toLatitude(value: Double): Latitude = Latitude(value)

    @TypeConverter fun fromLatitude(latitude: Latitude): Double = latitude.value
  }

  class LongitudeTypeConverter {
    @TypeConverter fun toLongitude(value: Double): Longitude = Longitude(value)

    @TypeConverter fun fromLongitude(longitude: Longitude): Double = longitude.value
  }
}

