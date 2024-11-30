package org.owntracks.android.ui.waypoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
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
import org.owntracks.android.test.ThresholdIdlingResourceInterface

@HiltViewModel
class WaypointsViewModel
@Inject
constructor(
    private val waypointsRepo: WaypointsRepo,
    private val locationProcessor: LocationProcessor,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @Named("waypointsEventCountingIdlingResource")
    private val waypointsEventCountingIdlingResource: ThresholdIdlingResourceInterface
) : ViewModel() {

  val waypointsFlow =
      flow {
            var currentWaypoints = waypointsRepo.getAll()
            waypointsEventCountingIdlingResource.set(currentWaypoints.size)
            emit(currentWaypoints)
            waypointsRepo.repoChangedEvent.collect { operation ->
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
                            removeIf { it.id == operation.waypoint.id }
                          }
                          is WaypointOperation.Clear -> {
                            clear()
                          }
                          is WaypointOperation.InsertMany -> {
                            addAll(operation.waypoints)
                          }
                        }
                      }
                      .sortedBy { it.tst }
              waypointsEventCountingIdlingResource.set(currentWaypoints.size)
              emit(currentWaypoints)
            }
          }
          .flowOn(ioDispatcher)
          //          .debounce(100.milliseconds)
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = emptyList())

  fun exportWaypoints() {
    viewModelScope.launch { locationProcessor.publishWaypointsMessage() }
  }
}
