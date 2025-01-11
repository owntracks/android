package org.owntracks.android.ui.waypoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

  val waypointsFlow =
      flow {
            var currentWaypoints = waypointsRepo.getAll()
            emit(currentWaypoints)
            waypointsRepo.repoChangedEvent.collect { operation ->
              Timber.tag("ARSE_WaypointsViewModel").d("Received waypointsUpdatedEvent $operation")
              currentWaypoints =
                  currentWaypoints
                      .toMutableList()
                      .apply {
                        when (operation) {
                          is WaypointOperation.Insert -> {
                            removeIf { it.tst == operation.waypoint.tst }
                            add(operation.waypoint)
                          }
                          is WaypointOperation.Update -> {
                            removeIf { it.tst == operation.waypoint.tst }
                            add(operation.waypoint)
                          }
                          is WaypointOperation.Delete -> {
                            remove(operation.waypoint)
                          }
                          is WaypointOperation.Clear -> {
                            clear()
                          }
                        }
                      }
                      .sortedBy { it.tst }

              Timber.tag("ARSE_WaypointsViewModel")
                  .d("Emitting updated waypoints: $currentWaypoints")
              emit(currentWaypoints)
            }
          }
          .flowOn(ioDispatcher)
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = emptyList())

  fun exportWaypoints() {
    viewModelScope.launch { locationProcessor.publishWaypointsMessage() }
  }
}
