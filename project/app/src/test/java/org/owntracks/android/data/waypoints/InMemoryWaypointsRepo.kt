package org.owntracks.android.data.waypoints

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryWaypointsRepo : WaypointsRepo() {
    private val waypoints = mutableListOf<WaypointModel>()

    override suspend fun get(id: Long): WaypointModel? {
        TODO("Not yet implemented")
    }

    override suspend fun getByTst(instant: Instant): WaypointModel? =
        waypoints.firstOrNull { it.tst == instant }

    override val all: List<WaypointModel>
        get() = waypoints

    private val mutableWaypoints = MutableSharedFlow<List<WaypointModel>>()
    override val allLive: Flow<List<WaypointModel>>
        get() = mutableWaypoints

    override suspend fun clearImpl() {
        waypoints.clear()
    }

    override suspend fun insertImpl(waypointModel: WaypointModel) {
        waypoints.add(waypointModel)
    }

    override suspend fun updateImpl(waypointModel: WaypointModel) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteImpl(waypointModel: WaypointModel) {
        TODO("Not yet implemented")
    }

    override val migrationCompleteFlow: StateFlow<Boolean>
        get() = MutableStateFlow(true)
}
