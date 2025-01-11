package org.owntracks.android.ui.waypoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.data.waypoints.WaypointsRepo.WaypointOperation
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.services.LocationProcessor
import timber.log.Timber

@HiltViewModel
class WaypointsViewModel
@Inject
constructor(
    private val waypointsRepo: WaypointsRepo,
    private val locationProcessor: LocationProcessor,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

  private val mutableWaypointsFlow = MutableStateFlow(emptyList<WaypointModel>())
  val waypointsFlow = mutableWaypointsFlow.asStateFlow()

  init {
    viewModelScope.launch(ioDispatcher) {
      mutableWaypointsFlow.emit(waypointsRepo.getAll())
      waypointsRepo.repoChangedEvent.collect { operation ->
        Timber.tag("ARSE_WaypointsViewModel").d("Received waypointsUpdatedEvent $operation")
        val currentWaypoints = mutableWaypointsFlow.value.toMutableList()
        when (operation) {
          is WaypointOperation.Insert ->
              currentWaypoints.apply {
                removeIf { it.tst == operation.waypoint.tst }
                add(operation.waypoint)
              }
          is WaypointOperation.Update ->
              currentWaypoints.apply {
                removeIf { it.tst == operation.waypoint.tst }
                add(operation.waypoint)
              }
          is WaypointOperation.Delete -> currentWaypoints.remove(operation.waypoint)
          is WaypointOperation.Clear -> currentWaypoints.clear()
        }
        Timber.tag("ARSE_WaypointsViewModel").d("Emitting $currentWaypoints")
        mutableWaypointsFlow.emit(currentWaypoints.sortedBy { it.tst })
      }
    }
  }

  fun exportWaypoints() {
    viewModelScope.launch { locationProcessor.publishWaypointsMessage() }
  }
}
