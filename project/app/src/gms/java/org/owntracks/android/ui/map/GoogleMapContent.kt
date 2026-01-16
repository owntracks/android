package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.location.toLatLng
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter

/**
 * Pure Compose implementation of Google Maps for the OwnTracks map screen. Replaces the
 * Fragment-based GoogleMapFragment with native Compose components using the maps-compose library.
 *
 * @param viewModel The MapViewModel containing all map state
 * @param contactImageBindingAdapter Adapter for loading contact avatar bitmaps
 * @param preferences Application preferences for map configuration
 * @param modifier Modifier for the composable
 */
@SuppressLint("MissingPermission")
@Composable
fun GoogleMapContent(
    viewModel: MapViewModel,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
  // Get initial camera position from ViewModel
  val initialPosition = remember { viewModel.initMapStartingLocation() }

  // Create camera position state
  val cameraPositionState = rememberCameraPositionState {
    position =
        CameraPosition.Builder()
            .target(initialPosition.latLng.toGMSLatLng())
            .zoom(convertStandardZoomToGoogleZoom(initialPosition.zoom).toFloat())
            .bearing(
                if (preferences.enableMapRotation) {
                  convertBetweenStandardRotationAndBearing(initialPosition.rotation)
                } else {
                  0f
                })
            .build()
  }

  // Observe map center for programmatic camera moves
  val mapCenter by viewModel.mapCenter.observeAsState()
  LaunchedEffect(mapCenter) {
    mapCenter?.let { center ->
      cameraPositionState.animate(CameraUpdateFactory.newLatLng(center.toGMSLatLng()))
    }
  }

  // Handle camera gestures - trigger map click when user pans/zooms
  LaunchedEffect(cameraPositionState) {
    snapshotFlow {
          cameraPositionState.isMoving to cameraPositionState.cameraMoveStartedReason
        }
        .collectLatest { (isMoving, reason) ->
          if (isMoving && reason == CameraMoveStartedReason.GESTURE) {
            viewModel.onMapClick()
          }
        }
  }

  // Update ViewModel when camera stops moving
  LaunchedEffect(cameraPositionState) {
    snapshotFlow { cameraPositionState.isMoving to cameraPositionState.position }
        .drop(1) // Skip initial value
        .collectLatest { (isMoving, position) ->
          if (!isMoving) {
            viewModel.setMapLocationFromMapMoveEvent(
                MapLocationZoomLevelAndRotation(
                    LatLng(
                        Latitude(position.target.latitude), Longitude(position.target.longitude)),
                    convertGoogleZoomToStandardZoom(position.zoom.toDouble()),
                    convertBetweenStandardRotationAndBearing(position.bearing)))
          }
        }
  }

  // Observe map layer style for map type changes
  val mapLayerStyle by viewModel.mapLayerStyle.observeAsState()

  // Observe current location for blue dot and sending location
  val currentLocation by viewModel.currentLocation.observeAsState()

  // Handle sending location when GPS fix becomes available
  val sendingLocation by viewModel.sendingLocation.observeAsState(false)
  LaunchedEffect(currentLocation, sendingLocation) {
    if (sendingLocation && currentLocation != null) {
      viewModel.onLocationAvailableWhileSending(currentLocation!!)
    }
  }

  // Update blue dot location in ViewModel
  LaunchedEffect(currentLocation) {
    currentLocation?.let { location -> viewModel.setCurrentBlueDotLocation(location.toLatLng()) }
  }

  // Follow device location in Device view mode
  val viewMode = viewModel.viewMode
  LaunchedEffect(currentLocation, viewMode) {
    if (viewMode == MapViewModel.ViewMode.Device && currentLocation != null) {
      cameraPositionState.animate(
          CameraUpdateFactory.newLatLng(currentLocation!!.toLatLng().toGMSLatLng()))
    }
  }

  // Collect contacts with a version counter to trigger recomposition
  val contactsVersion by
      produceState(0) {
        viewModel.contactUpdatedEvent.collect {
          value++ // Increment to trigger recomposition on any contact change
        }
      }

  // Waypoints state - initially empty, populated from suspending call and updates
  val waypoints by
      produceState<Set<WaypointModel>>(emptySet()) {
        // Initial load
        value = viewModel.getAllWaypoints().toSet()

        // Observe updates
        viewModel.waypointUpdatedEvent.collect { operation ->
          value =
              when (operation) {
                is WaypointsRepo.WaypointOperation.Clear -> emptySet()
                is WaypointsRepo.WaypointOperation.Delete -> value - operation.waypoint
                is WaypointsRepo.WaypointOperation.Insert -> value + operation.waypoint
                is WaypointsRepo.WaypointOperation.Update ->
                    value.filterNot { it.id == operation.waypoint.id }.toSet() + operation.waypoint
                is WaypointsRepo.WaypointOperation.InsertMany ->
                    value + operation.waypoints.toSet()
              }
        }
      }

  // Get map style for dark mode
  val mapStyleOptions = rememberMapStyleOptions()

  // Determine map type from layer style
  val mapType =
      when (mapLayerStyle) {
        MapLayerStyle.GoogleMapDefault -> MapType.NORMAL
        MapLayerStyle.GoogleMapHybrid -> MapType.HYBRID
        MapLayerStyle.GoogleMapSatellite -> MapType.SATELLITE
        MapLayerStyle.GoogleMapTerrain -> MapType.TERRAIN
        else -> MapType.NORMAL
      }

  // Check if we have location permission
  val hasLocationPermission = viewModel.hasLocationPermission()

  GoogleMap(
      modifier = modifier.fillMaxSize(),
      cameraPositionState = cameraPositionState,
      properties =
          MapProperties(
              isMyLocationEnabled = hasLocationPermission,
              mapType = mapType,
              mapStyleOptions = mapStyleOptions,
              minZoomPreference = 4f,
              maxZoomPreference = 20f,
              isIndoorEnabled = false),
      uiSettings =
          MapUiSettings(
              myLocationButtonEnabled = false,
              compassEnabled = preferences.enableMapRotation,
              rotationGesturesEnabled = preferences.enableMapRotation,
              zoomControlsEnabled = false,
              mapToolbarEnabled = false),
      onMapClick = { viewModel.onMapClick() }) {
        // Render contact markers - use key to force recomposition on contact changes
        androidx.compose.runtime.key(contactsVersion) {
          ContactMarkers(
              contacts = viewModel.allContacts,
              contactImageBindingAdapter = contactImageBindingAdapter,
              onMarkerClick = { id -> viewModel.onMarkerClick(id) })
        }

        // Render region overlays
        RegionOverlays(waypoints = waypoints, showRegions = preferences.showRegionsOnMap)
      }

  // Notify ViewModel that map is ready
  LaunchedEffect(Unit) { viewModel.onMapReady() }
}
