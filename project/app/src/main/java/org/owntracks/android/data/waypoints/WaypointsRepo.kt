package org.owntracks.android.data.waypoints

import android.content.Context
import com.growse.lmdb_kt.Environment
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.support.MessageWaypointCollection
import timber.log.Timber

abstract class WaypointsRepo
protected constructor(
    private val scope: CoroutineScope,
    private val applicationContext: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
  init {
    val handler = CoroutineExceptionHandler { _, exception ->
      Timber.e(exception, "Error migrating waypoints")
    }
    scope.launch(ioDispatcher + handler) { migrateFromLegacyStorage() }
  }

  sealed class WaypointOperation {
    data class Insert(val waypoint: WaypointModel) : WaypointOperation()

    data class Update(val waypoint: WaypointModel) : WaypointOperation()

    data class Delete(val waypoint: WaypointModel) : WaypointOperation()

    data object Clear : WaypointOperation()
  }

  abstract suspend fun get(id: Long): WaypointModel?

  abstract suspend fun getByTst(instant: Instant): WaypointModel?

  abstract val all: List<WaypointModel>

  private val mutableOperations = MutableSharedFlow<WaypointOperation>()
  val operations: SharedFlow<WaypointOperation> = mutableOperations

  suspend fun insert(waypointModel: WaypointModel) {
    waypointModel.run {
      insertImpl(this@run)
      mutableOperations.emit(WaypointOperation.Insert(this@run))
    }
  }

  suspend fun update(waypointModel: WaypointModel, notify: Boolean) {
    waypointModel.run {
      updateImpl(this@run)
      if (notify) {
        mutableOperations.emit(WaypointOperation.Update(this@run))
      }
    }
  }

  suspend fun delete(waypointModel: WaypointModel) {
    waypointModel.run {
      deleteImpl(this@run)
      mutableOperations.emit(WaypointOperation.Delete(this@run))
    }
  }

  suspend fun clearAll() {
    clearImpl()
    mutableOperations.emit(WaypointOperation.Clear)
  }

  /**
   * Imports all the waypoints in a given message, replacing
   *
   * @param waypoints
   */
  suspend fun importFromMessage(waypoints: MessageWaypointCollection?) {
    waypoints
        ?.filter {
          // check if the latitude and longitude are valid, otherwise do not replace the waypoint
          ((it.latitude >= -90.0) &&
                  (it.latitude <= 90.0) &&
                  (it.longitude >= -180.0) &&
                  (it.longitude <= 180.0))
              .also { valid ->
                if (!valid)
                    Timber.w(
                        "Ignoring waypoint with invalid coordinates: ${it.description}",
                    )
              }
        }
        ?.map { toDaoObject(it) }
        ?.run { insertAllImpl(this) }
  }

  private fun toDaoObject(messageWaypoint: MessageWaypoint): WaypointModel {
    return WaypointModel(
        0,
        messageWaypoint.description ?: "",
        Latitude(messageWaypoint.latitude),
        Longitude(messageWaypoint.longitude),
        messageWaypoint.radius ?: 0,
        Instant.MIN,
        0,
        Instant.ofEpochSecond(messageWaypoint.timestamp))
  }

  fun fromDaoObject(w: WaypointModel): MessageWaypoint {
    return MessageWaypoint().apply {
      description = w.description
      latitude = w.geofenceLatitude.value
      longitude = w.geofenceLongitude.value
      radius = w.geofenceRadius
      timestamp = w.tst.epochSecond
    }
  }

  protected abstract suspend fun clearImpl()

  protected abstract suspend fun insertImpl(waypointModel: WaypointModel)

  protected abstract suspend fun insertAllImpl(waypoints: List<WaypointModel>)

  protected abstract suspend fun updateImpl(waypointModel: WaypointModel)

  protected abstract suspend fun deleteImpl(waypointModel: WaypointModel)

  abstract val migrationCompleteFlow: StateFlow<Boolean>

  private suspend fun migrateFromLegacyStorage() {
    try {
      val objectboxPath = applicationContext.filesDir.resolve("objectbox/objectbox")
      if (objectboxPath.exists() && objectboxPath.canRead() && objectboxPath.isDirectory) {
        val migrationDuration = measureTimedValue {
          Environment(
                  applicationContext.filesDir.resolve("objectbox/objectbox").toString(),
                  readOnly = true,
                  locking = false)
              .use { lmdb ->
                lmdb
                    .beginTransaction()
                    .dump()
                    .filter { entry ->
                      // This is the magic we're looking for at the start of an LMDB key to
                      // indicate it's a waypoint
                      entry.key.bytes.slice(0..3) == listOf<Byte>(0x18, 0, 0, 0x4)
                    }
                    .map { lmdbEntry -> deserializeWaypointModel(lmdbEntry.value) }
                    .toList()
                    .run {
                      insertAllImpl(this)
                      Timber.tag("ARSE_WaypointsRepo").d("Migrated ${this.size} waypoints")
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
        //          db.waypointDao().getRowCount().let { rowCount ->
        //            Timber.i(
        //                "Waypoints Migration complete in ${migrationDuration.duration}. Tried to
        // insert ${migrationDuration.value}, ended up with $rowCount waypoints in the repo")
        //          }
      }
    } catch (e: Throwable) {
      Timber.tag("ARSE_RoomWaypointsRepo").e(e, "Error migrating waypoints")
    } finally {
      //        _migrationCompleteFlow.compareAndSet(expect = false, update = true)
    }
  }

  private fun deserializeWaypointModel(value: ByteArray): WaypointModel =
      ByteBuffer.wrap(value)
          .run { FbWaypointModel.getRootAsFbWaypointModel(this) }
          .run {
            WaypointModel(
                id,
                description ?: "",
                Latitude(geofenceLatitude),
                Longitude(geofenceLongitude),
                geofenceRadius,
                if (lastTriggered == 0L) null else Instant.ofEpochSecond(lastTriggered),
                lastTransition,
                Instant.ofEpochSecond(tst))
          }
}
