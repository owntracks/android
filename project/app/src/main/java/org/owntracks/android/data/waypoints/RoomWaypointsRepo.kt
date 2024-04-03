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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.google.flatbuffers.Constants
import com.google.flatbuffers.FlatBufferBuilder
import com.google.flatbuffers.Table
import com.growse.lmdb_kt.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
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
class RoomWaypointsRepo
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : WaypointsRepo() {
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

    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertAll(waypoints: List<WaypointModel>)

    @Query("SELECT COUNT(*) FROM WaypointModel") fun getRowCount(): Int
  }

  @Database(entities = [WaypointModel::class], version = 1)
  @TypeConverters(LocalDateTimeConverter::class)
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

  override suspend fun updateImpl(waypointModel: WaypointModel) =
      withContext(ioDispatcher) { db.waypointDao().upsert(waypointModel) }

  override suspend fun deleteImpl(waypointModel: WaypointModel) =
      withContext(ioDispatcher) { db.waypointDao().delete(waypointModel) }

  private val _migrationCompleteFlow = MutableStateFlow(false)
  override val migrationCompleteFlow: StateFlow<Boolean> = _migrationCompleteFlow

  @OptIn(ExperimentalTime::class)
  fun migrateFromLegacyStorage(): Job {
    val handler = CoroutineExceptionHandler { _, exception ->
      Timber.e(exception, "Error migrating waypoints")
    }
    return scope.launch(ioDispatcher + handler) {
      try {
        val objectboxPath = applicationContext.filesDir.resolve("objectbox/objectbox")
        if (objectboxPath.exists() && objectboxPath.canRead() && objectboxPath.isDirectory) {
          val migrationDuration = measureTimedValue {
            Environment(
                    applicationContext.filesDir.resolve("objectbox/objectbox").toString(),
                    readOnly = true,
                    locking = false)
                .use {
                  it.beginTransaction()
                      .dump()
                      .filter { entry ->
                        // This is the magic we're looking for at the start of an LMDB key to
                        // indicate it's a waypoint
                        entry.key.bytes.slice(0..3) == listOf<Byte>(0x18, 0, 0, 0x4)
                      }
                      .map { lmdbEntry -> deserializeWaypointModel(lmdbEntry.value) }
                      .toList()
                      .run {
                        db.waypointDao().insertAll(this)
                        this.size
                      }
                      .run {
                        val deleted =
                            applicationContext.filesDir.resolve("objectbox").deleteRecursively()
                        Timber.d("Deleting legacy waypoints database file. Success=$deleted")
                        this
                      }
                }
          }
          db.waypointDao().getRowCount().let { rowCount ->
            Timber.i(
                "Waypoints Migration complete in ${migrationDuration.duration}. Tried to insert ${migrationDuration.value}, ended up with $rowCount waypoints in the repo")
          }
        }
      } finally {
        _migrationCompleteFlow.compareAndSet(expect = false, update = true)
      }
    }
  }

  private fun deserializeWaypointModel(value: ByteArray): WaypointModel =
      ByteBuffer.wrap(value)
          .run { FbWaypointModel.getRootAsFbWaypointModel(this) }
          .run {
            WaypointModel(
                id,
                description ?: "",
                geofenceLatitude,
                geofenceLongitude,
                geofenceRadius,
                if (lastTriggered == 0L) null else Instant.ofEpochSecond(lastTriggered),
                lastTransition,
                Instant.ofEpochSecond(tst))
          }

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

  @Suppress("unused")
  class FbWaypointModel : Table() {

    fun init(_i: Int, _bb: ByteBuffer) {
      __reset(_i, _bb)
    }

    fun assign(_i: Int, _bb: ByteBuffer): FbWaypointModel {
      init(_i, _bb)
      return this
    }

    val id: Long
      get() {
        val o = __offset(4)
        return if (o != 0) bb.getLong(o + bb_pos) else 0L
      }

    val description: String?
      get() {
        val o = __offset(6)
        return if (o != 0) {
          __string(o + bb_pos)
        } else {
          null
        }
      }

    val descriptionAsByteBuffer: ByteBuffer
      get() = __vector_as_bytebuffer(6, 1)

    fun descriptionInByteBuffer(_bb: ByteBuffer): ByteBuffer = __vector_in_bytebuffer(_bb, 6, 1)

    val geofenceLatitude: Double
      get() {
        val o = __offset(8)
        return if (o != 0) bb.getDouble(o + bb_pos) else 0.0
      }

    val geofenceLongitude: Double
      get() {
        val o = __offset(10)
        return if (o != 0) bb.getDouble(o + bb_pos) else 0.0
      }

    val geofenceRadius: Int
      get() {
        val o = __offset(12)
        return if (o != 0) bb.getInt(o + bb_pos) else 0
      }

    val lastTriggered: Long
      get() {
        val o = __offset(14)
        return if (o != 0) bb.getLong(o + bb_pos) else 0L
      }

    val lastTransition: Int
      get() {
        val o = __offset(16)
        return if (o != 0) bb.getInt(o + bb_pos) else 0
      }

    val tst: Long
      get() {
        val o = __offset(18)
        return if (o != 0) bb.getLong(o + bb_pos) else 0L
      }

    override fun toString(): String {
      return "FbWaypointModel(id=$id,description=$description,latitude=$geofenceLatitude,longitude=$geofenceLongitude,radius=$geofenceRadius,lastTransition=$lastTransition,lastTriggered=$lastTriggered,tst=$tst)"
    }

    companion object {
      fun validateVersion() = Constants.FLATBUFFERS_23_5_26()

      fun getRootAsFbWaypointModel(_bb: ByteBuffer): FbWaypointModel =
          getRootAsFbWaypointModel(_bb, FbWaypointModel())

      private fun getRootAsFbWaypointModel(_bb: ByteBuffer, obj: FbWaypointModel): FbWaypointModel {
        _bb.order(ByteOrder.LITTLE_ENDIAN)
        return (obj.assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
      }

      fun createFbWaypointModel(
          builder: FlatBufferBuilder,
          id: Long,
          descriptionOffset: Int,
          geofenceLatitude: Double,
          geofenceLongitude: Double,
          geofenceRadius: Int,
          lastTriggered: Long,
          lastTransition: Int,
          tst: Long
      ): Int {
        builder.startTable(8)
        addTst(builder, tst)
        addLastTriggered(builder, lastTriggered)
        addGeofenceLongitude(builder, geofenceLongitude)
        addGeofenceLatitude(builder, geofenceLatitude)
        addId(builder, id)
        addLastTransition(builder, lastTransition)
        addGeofenceRadius(builder, geofenceRadius)
        addDescription(builder, descriptionOffset)
        return endFbWaypointModel(builder)
      }

      fun startFbWaypointModel(builder: FlatBufferBuilder) = builder.startTable(8)

      private fun addId(builder: FlatBufferBuilder, id: Long) = builder.addLong(0, id, 0L)

      private fun addDescription(builder: FlatBufferBuilder, description: Int) =
          builder.addOffset(1, description, 0)

      private fun addGeofenceLatitude(builder: FlatBufferBuilder, geofenceLatitude: Double) =
          builder.addDouble(2, geofenceLatitude, 0.0)

      private fun addGeofenceLongitude(builder: FlatBufferBuilder, geofenceLongitude: Double) =
          builder.addDouble(3, geofenceLongitude, 0.0)

      private fun addGeofenceRadius(builder: FlatBufferBuilder, geofenceRadius: Int) =
          builder.addInt(4, geofenceRadius, 0)

      private fun addLastTriggered(builder: FlatBufferBuilder, lastTriggered: Long) =
          builder.addLong(5, lastTriggered, 0L)

      private fun addLastTransition(builder: FlatBufferBuilder, lastTransition: Int) =
          builder.addInt(6, lastTransition, 0)

      private fun addTst(builder: FlatBufferBuilder, tst: Long) = builder.addLong(7, tst, 0L)

      private fun endFbWaypointModel(builder: FlatBufferBuilder): Int {
        return builder.endTable()
      }

      fun finishFbWaypointModelBuffer(builder: FlatBufferBuilder, offset: Int) =
          builder.finish(offset)

      fun finishSizePrefixedFbWaypointModelBuffer(builder: FlatBufferBuilder, offset: Int) =
          builder.finishSizePrefixed(offset)
    }
  }
}
// automatically generated by the FlatBuffers compiler, do not modify
