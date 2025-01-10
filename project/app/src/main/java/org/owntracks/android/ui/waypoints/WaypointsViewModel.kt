package org.owntracks.android.ui.waypoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.services.LocationProcessor

@HiltViewModel
class WaypointsViewModel
@Inject
constructor(waypointsRepo: WaypointsRepo, private val locationProcessor: LocationProcessor) :
    ViewModel() {

  val waypoints: StateFlow<List<WaypointModel>> = waypointsRepo.allLive
      .stateIn(
          scope = viewModelScope,
          started = SharingStarted.Lazily,
          initialValue = emptyList()
      )


  fun exportWaypoints() {
    viewModelScope.launch { locationProcessor.publishWaypointsMessage() }
  }
}
