package org.owntracks.android.data.waypoints

import android.content.Context
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.owntracks.android.test.SimpleIdlingResource

class InMemoryWaypointsRepo(
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope,
    applicationContext: Context,
    @Suppress("UNUSED_PARAMETER") ioDispatcher: CoroutineDispatcher
) :
    WaypointsRepo(
        applicationContext, SimpleIdlingResource("waypointsMigrationIdlingResource", false)) {
  private val waypoints = mutableListOf<WaypointModel>()

  override suspend fun get(id: Long): WaypointModel? {
    TODO("Not yet implemented")
  }

  override suspend fun getByTst(instant: Instant): WaypointModel? =
      waypoints.firstOrNull { it.tst == instant }

  override suspend fun getAll(): List<WaypointModel> = waypoints

  override suspend fun clearImpl() {
    waypoints.clear()
  }

  override suspend fun insertImpl(waypointModel: WaypointModel): Long {
    waypoints.add(waypointModel)
    return waypointModel.id
  }

  override suspend fun insertAllImpl(waypoints: List<WaypointModel>) {
    this.waypoints.addAll(waypoints)
  }

  override suspend fun updateImpl(waypointModel: WaypointModel): Long {
    TODO("Not yet implemented")
  }

  override suspend fun deleteImpl(waypointModel: WaypointModel) {
    TODO("Not yet implemented")
  }

  override val migrationCompleteFlow: StateFlow<Boolean>
    get() = MutableStateFlow(true)
}
