package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.MapView
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.location.toLatLng
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.ui.map.osm.OSMMapFragment
import timber.log.Timber
import timber.log.Timber.Forest.tag

class GoogleMapFragment
internal constructor(
    private val preferences: Preferences,
    contactImageBindingAdapter: ContactImageBindingAdapter
) :
    MapFragment(contactImageBindingAdapter, preferences),
    OnMapReadyCallback,
    OnMapsSdkInitializedCallback {

  data class WaypointOnMap(val marker: Marker, val circle: Circle)

  override val layout: Int
    get() = R.layout.google_map_fragment

  private lateinit var googleMapView: MapView

  private val googleMapLocationSource: LocationSource by lazy {
    object : LocationSource {
      private var locationObserver: Observer<Location>? = null

      override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
        locationObserver =
            Observer { location: Location ->
                  onLocationObserved(location) {
                    onLocationChangedListener.onLocationChanged(location)
                  }
                }
                .apply { viewModel.currentLocation.observe(viewLifecycleOwner, this) }
      }

      override fun deactivate() {
        locationObserver?.run(viewModel.currentLocation::removeObserver)
      }
    }
  }

  private var googleMap: GoogleMap? = null
  private val markersOnMap: MutableMap<String, Marker> = HashMap()
  private val regionsOnMap: MutableMap<Long, WaypointOnMap> = mutableMapOf()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val root = super.onCreateView(inflater, container, savedInstanceState)
    googleMapView = root.findViewById(R.id.google_map_view)
    googleMapView.onCreate(savedInstanceState)
    googleMapView.getMapAsync(this)
    return root
  }

  override fun onMapReady(googleMap: GoogleMap) {
    this.googleMap = googleMap
    initMap()
  }

  private fun setMapStyle() {
    if (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES) {
      googleMap?.setMapStyle(
          MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.google_maps_night_theme))
    }
  }

  private fun MapLocationZoomLevelAndRotation.toCameraUpdate(): CameraUpdate =
      CameraUpdateFactory.newCameraPosition(
          CameraPosition.builder()
              .target(this.latLng.toGMSLatLng())
              .zoom(convertStandardZoomToGoogleZoom(this.zoom).toFloat())
              .bearing(
                  if (preferences.enableMapRotation) {
                    convertBetweenStandardRotationAndBearing(this.rotation)
                  } else {
                    0f
                  })
              .build())

  override fun initMap() {
    MapsInitializer.initialize(requireContext(), MapsInitializer.Renderer.LATEST, this)
    this.googleMap?.run {
      val myLocationEnabled = viewModel.hasLocationPermission()
      if (viewModel.hasLocationPermission()) {
        Timber.d("GoogleMapFragment initMap hasLocationCapability=$myLocationEnabled")
        @SuppressLint("MissingPermission") // We've guarded this already
        isMyLocationEnabled = myLocationEnabled
      }

      setMaxZoomPreference(MAX_ZOOM_LEVEL.toFloat())
      setMinZoomPreference(MIN_ZOOM_LEVEL.toFloat())
      isIndoorEnabled = false
      uiSettings.isMyLocationButtonEnabled = false
      uiSettings.setAllGesturesEnabled(true)
      preferences.enableMapRotation.run {
        uiSettings.isCompassEnabled = this
        uiSettings.isRotateGesturesEnabled = this
      }

      setLocationSource(googleMapLocationSource)

      setMapStyle()

      viewModel.initMapStartingLocation().run { moveCamera(toCameraUpdate()) }

      setOnMarkerClickListener {
        it.tag?.run {
          onMarkerClicked(this as String)
          true
        } ?: false
      }

      setOnMapClickListener { onMapClick() }
      setOnCameraMoveStartedListener { reason ->
        if (reason == REASON_GESTURE) {
          onMapClick()
        }
      }

      setOnCameraIdleListener {
        viewModel.setMapLocationFromMapMoveEvent(
            this.cameraPosition.run {
              MapLocationZoomLevelAndRotation(
                  LatLng(Latitude(target.latitude), Longitude(target.longitude)),
                  convertGoogleZoomToStandardZoom(zoom.toDouble()),
                  convertBetweenStandardRotationAndBearing(bearing))
            })
      }

      viewModel.mapLayerStyle.value?.run { setMapLayerType(this) }
      drawAllContactsAndRegions()
    }
  }

  override fun updateCamera(latLng: LatLng) {
    googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng.toGMSLatLng()))
  }

  override fun updateMarkerOnMap(id: String, latLng: LatLng, image: Bitmap) {
    googleMap?.run { // If we don't have a google Map, we can't add markers to it
      // Remove null markers from the collection
      markersOnMap.values.removeAll { it.tag == null }
      markersOnMap
          .getOrPut(id) {
            addMarker(
                    MarkerOptions()
                        .position(latLng.toGMSLatLng())
                        .anchor(0.5f, 0.5f)
                        .visible(false))!!
                .also { it.tag = id }
          }
          .run {
            position = latLng.toGMSLatLng()
            setIcon(BitmapDescriptorFactory.fromBitmap(image))
            isVisible = true
          }
    }
  }

  override fun removeMarkerFromMap(id: String) {
    markersOnMap[id]?.remove()
  }

  override fun currentMarkersOnMap(): Set<String> = markersOnMap.keys

  override fun onResume() {
    super.onResume()
    googleMapView.onResume()
    setMapStyle()
  }

  override fun onLowMemory() {
    googleMapView.onLowMemory()
    super.onLowMemory()
  }

  override fun onPause() {
    googleMapView.onPause()
    super.onPause()
  }

  override fun onDestroy() {
    googleMapView.onDestroy()
    super.onDestroy()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    googleMapView.onSaveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onStart() {
    super.onStart()
    googleMapView.onStart()
  }

  override fun onStop() {
    googleMapView.onStop()
    super.onStop()
  }

  override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
    Timber.d("Maps SDK initialized with renderer: ${renderer.name}")
  }

  override fun addRegion(waypoint: WaypointModel) {
    if (preferences.showRegionsOnMap) {
      googleMap?.run {
        Timber.d("Adding region to map $waypoint")
        WaypointOnMap(
                MarkerOptions()
                    .apply {
                      position(waypoint.getLocation().toLatLng().toGMSLatLng())
                      anchor(0.5f, 1.0f)
                      title(waypoint.description)
                    }
                    .let { addMarker(it)!! },
                CircleOptions()
                    .apply {
                      center(waypoint.getLocation().toLatLng().toGMSLatLng())
                      radius(waypoint.geofenceRadius.toDouble())
                      fillColor(getRegionColor())
                      strokeWidth(1.0f)
                    }
                    .let { addCircle(it) })
            .run { regionsOnMap.put(waypoint.id, this) }
      }
    }
  }

  override fun deleteRegion(waypoint: WaypointModel) {
    if (preferences.showRegionsOnMap) {
      googleMap?.run {
        Timber.d("Deleting region from map $waypoint")
        regionsOnMap[waypoint.id]?.run {
          circle.remove()
          marker.remove()
        }
      }
    }
  }

  override fun updateRegion(waypoint: WaypointModel) {
    if (preferences.showRegionsOnMap) {
      googleMap?.run {
        Timber.d("Updating region on map $waypoint")
        regionsOnMap[waypoint.id]?.run {
          circle.center = waypoint.getLocation().toLatLng().toGMSLatLng()
          circle.radius = waypoint.geofenceRadius.toDouble()
          marker.position = waypoint.getLocation().toLatLng().toGMSLatLng()
          marker.title = waypoint.description
        }
      }
    }
  }

  override fun reDrawRegions(regions: Set<WaypointModel>) {
    if (preferences.showRegionsOnMap) {
      googleMap?.run {
        Timber.d("Drawing regions on map: $regions")
        regionsOnMap.values.forEach {
          it.circle.remove()
          it.marker.remove()
        }
        regions.forEach(::addRegion)
      }
    }
  }

  companion object {
    private const val MIN_ZOOM_LEVEL: Double = 4.0
    private const val MAX_ZOOM_LEVEL: Double = 20.0
  }

  /**
   * Convert standard rotation to google bearing. OSM uses a "map rotation" concept to represent how
   * the map is oriented, whereas google uses the "bearing". These are not the same thing, so this
   * converts from a rotation to a bearing and back again (because it's reversable)
   *
   * @param input
   * @return an equivalent bearing
   */
  private fun convertBetweenStandardRotationAndBearing(input: Float): Float = -input % 360

  /**
   * Converts standard (OSM) zoom to Google Maps zoom level. Simple linear conversion
   *
   * @param inputZoom Zoom level from standard (OSM)
   * @return Equivalent zoom level on Google Maps
   */
  private fun convertStandardZoomToGoogleZoom(inputZoom: Double): Double =
      linearConversion(
          OSMMapFragment.MIN_ZOOM_LEVEL..OSMMapFragment.MAX_ZOOM_LEVEL,
          MIN_ZOOM_LEVEL..MAX_ZOOM_LEVEL,
          inputZoom)

  /**
   * Converts Google Maps zoom to Standard (OSM) zoom level. Simple linear conversion
   *
   * @param inputZoom Zoom level from Google Maps
   * @return Equivalent zoom level on Standard (OSM)
   */
  private fun convertGoogleZoomToStandardZoom(inputZoom: Double): Double =
      linearConversion(
          MIN_ZOOM_LEVEL..MAX_ZOOM_LEVEL,
          OSMMapFragment.MIN_ZOOM_LEVEL..OSMMapFragment.MAX_ZOOM_LEVEL,
          inputZoom)

  /**
   * Linear conversion of a point in a range to the equivalent point in another range
   *
   * @param fromRange Starting range the given point is in
   * @param toRange Range to translate the point to
   * @param point point in the starting range
   * @return a value that's at the same location in [toRange] as [point] is in [fromRange]
   */
  fun linearConversion(
      fromRange: ClosedRange<Double>,
      toRange: ClosedRange<Double>,
      point: Double
  ): Double {
    if (!fromRange.contains(point)) {
      throw Exception("Given point $point is not in fromRange $fromRange")
    }
    return ((point - fromRange.start) / (fromRange.endInclusive - fromRange.start)) *
        (toRange.endInclusive - toRange.start) + toRange.start
  }

  override fun setMapLayerType(mapLayerStyle: MapLayerStyle) {
    when (mapLayerStyle) {
      MapLayerStyle.GoogleMapDefault -> {
        googleMap?.mapType = MAP_TYPE_NORMAL
      }
      MapLayerStyle.GoogleMapHybrid -> {
        googleMap?.mapType = MAP_TYPE_HYBRID
      }
      MapLayerStyle.GoogleMapSatellite -> {
        googleMap?.mapType = MAP_TYPE_SATELLITE
      }
      MapLayerStyle.GoogleMapTerrain -> {
        googleMap?.mapType = MAP_TYPE_TERRAIN
      }
      else -> {
        Timber.w("Unsupported map layer type $mapLayerStyle")
      }
    }
  }
}
