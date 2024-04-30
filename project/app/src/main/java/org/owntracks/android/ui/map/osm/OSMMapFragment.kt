package org.owntracks.android.ui.map.osm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.display.DisplayManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import kotlin.math.roundToInt
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.IOrientationConsumer
import org.osmdroid.views.overlay.compass.IOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.OsmMapFragmentBinding
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toGeoPoint
import org.owntracks.android.location.toLatLng
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.ui.map.MapFragment
import org.owntracks.android.ui.map.MapLayerStyle
import org.owntracks.android.ui.map.MapLocationZoomLevelAndRotation
import timber.log.Timber

class OSMMapFragment
internal constructor(
    private val preferences: Preferences,
    contactImageBindingAdapter: ContactImageBindingAdapter
) : MapFragment<OsmMapFragmentBinding>(contactImageBindingAdapter, preferences) {
  override val layout: Int
    get() = R.layout.osm_map_fragment

  private val osmMapLocationSource: IMyLocationProvider =
      object : IMyLocationProvider {
        private var locationObserver: Observer<Location>? = null

        override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
          val locationProvider: IMyLocationProvider = this
          locationObserver =
              Observer { location: Location ->
                    onLocationObserved(location) {
                      myLocationConsumer?.onLocationChanged(location, locationProvider)
                    }
                  }
                  .apply { viewModel.currentLocation.observe(viewLifecycleOwner, this) }
          return true
        }

        override fun stopLocationProvider() {
          locationObserver?.run(viewModel.currentLocation::removeObserver)
        }

        override fun getLastKnownLocation(): Location? {
          return viewModel.currentLocation.value
        }

        override fun destroy() {
          stopLocationProvider()
        }
      }

  private var mapView: MapView? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    Configuration.getInstance().apply {
      load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
      osmdroidBasePath.resolve("tiles").run {
        if (exists()) {
          deleteRecursively()
        }
      }
      osmdroidTileCache = requireContext().noBackupFilesDir.resolve("osmdroid/tiles")
    }
    val view = super.onCreateView(inflater, container, savedInstanceState)
    initMap()
    return view
  }

  private fun setMapStyle() {
    if (resources.configuration.uiMode.and(android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES) {
      mapView?.run { overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS) }
    } else {
      mapView?.run { overlayManager.tilesOverlay.setColorFilter(null) }
    }
  }

  private val mapListener =
      DelayedMapListener(
          object : MapListener {
            private fun updateViewModelMapLocation() {
              mapView?.run {
                viewModel.setMapLocationFromMapMoveEvent(
                    MapLocationZoomLevelAndRotation(
                        LatLng(mapCenter.latitude, mapCenter.longitude),
                        zoomLevelDouble,
                        mapOrientation))
              }
            }

            override fun onScroll(event: ScrollEvent?): Boolean {
              updateViewModelMapLocation()
              return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
              updateViewModelMapLocation()
              return true
            }
          })

  class MapRotationOrientationProvider(context: Context) : IOrientationProvider {
    private val display = context.safeGetDisplay()
    private var myOrientationConsumer: IOrientationConsumer? = null
    private var lastOrientation = 0f

    fun updateOrientation(orientation: Float) {
      lastOrientation = -(orientation + displayRotationToDegrees())
      myOrientationConsumer?.onOrientationChanged(lastOrientation, this)
    }

    private fun displayRotationToDegrees(): Float =
        when (display?.rotation) {
          Surface.ROTATION_0 -> 0f
          Surface.ROTATION_90 -> 90f
          Surface.ROTATION_180 -> 180f
          Surface.ROTATION_270 -> 270f
          else -> 0f
        }

    override fun startOrientationProvider(orientationConsumer: IOrientationConsumer?): Boolean {
      myOrientationConsumer = orientationConsumer
      return true
    }

    override fun stopOrientationProvider() {}

    override fun getLastKnownOrientation(): Float = lastOrientation

    override fun destroy() {}

    private fun Context.safeGetDisplay(): Display? {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display
      } else {
        ((getSystemService(Context.DISPLAY_SERVICE)) as DisplayManager).displays.firstOrNull()
      }
    }
  }

  val orientationProvider by lazy { MapRotationOrientationProvider(requireContext()) }
  private val compassOrientationMapListener =
      object : MapListener {
        private fun updateOrientation() {
          mapView?.mapOrientation?.run { orientationProvider.updateOrientation(this) }
        }

        override fun onScroll(event: ScrollEvent?): Boolean {
          updateOrientation()
          return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
          updateOrientation()
          return true
        }
      }

  override fun initMap() {
    val myLocationEnabled = viewModel.hasLocationPermission()
    Timber.d("OSMMapFragment initMap locationEnabled=$myLocationEnabled")
    mapView =
        this.binding.osmMapView.apply {
          minZoomLevel = MIN_ZOOM_LEVEL
          maxZoomLevel = MAX_ZOOM_LEVEL
          viewModel.mapLayerStyle.value?.run { setMapLayerType(this) }
          zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
          addMapListener(mapListener)
          zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
          // Make sure we don't add to the mylocation overlay
          if (!overlays.any {
            it is MyLocationNewOverlay && it.mMyLocationProvider == osmMapLocationSource
          }) {
            overlays.add(
                MyLocationNewOverlay(osmMapLocationSource, this).apply {
                  setOnClickListener { onMapClick() }
                  setOnTouchListener { v, event ->
                    if (event.action == ACTION_BUTTON_RELEASE) {
                      v.performClick()
                    }
                    onMapClick()
                    false
                  }
                  val bitmapDimension = resources.displayMetrics.density * 24
                  val dot =
                      ResourcesCompat.getDrawable(resources, R.drawable.location_dot, null)
                          ?.toBitmap(bitmapDimension.roundToInt(), bitmapDimension.roundToInt())
                  val arrow =
                      ResourcesCompat.getDrawable(resources, R.drawable.location_dot_arrow, null)
                          ?.toBitmap(bitmapDimension.roundToInt(), bitmapDimension.roundToInt())
                  setDirectionIcon(arrow)
                  setPersonIcon(dot)
                  setPersonAnchor(0.5f, 0.5f)
                  setDirectionAnchor(0.5f, 0.5f)
                })
          }

          if (!overlays.any { it is RotationGestureOverlay } && preferences.enableMapRotation) {
            overlays.add(RotationGestureOverlay(this))
          }
          if (!overlays.any { it is CopyrightOverlay }) {
            overlays.add(CopyrightOverlay(context))
          }
          if (!overlays.any { it is CompassOverlay } && preferences.enableMapRotation) {
            addMapListener(compassOrientationMapListener)

            val compassMargin = 35f

            overlays.add(
                ClickableCompassOverlay(
                        requireContext().applicationContext, orientationProvider, this)
                    .apply {
                      isPointerMode = false
                      enableCompass()
                      setCompassCenter(compassMargin, compassMargin)
                    })
          }
          if (!overlays.any { it is ScaleBarOverlay }) {
            overlays.add(ScaleBarOverlay(this))
          }
          setMultiTouchControls(true)
          isTilesScaledToDpi = true
          tilesScaleFactor = preferences.osmTileScaleFactor
          viewModel.initMapStartingLocation().run {
            controller.animateTo(latLng.toGeoPoint(), zoom, 0, rotation)
          }
        }
    setMapStyle()
    drawAllContactsAndRegions()
  }

  override fun updateCamera(latLng: LatLng) {
    mapView?.controller?.run { animateTo(latLng.toGeoPoint()) }
  }

  override fun updateMarkerOnMap(id: String, latLng: LatLng, image: Bitmap) {
    mapView?.run {
      val existingMarker: Marker? = overlays.firstOrNull { it is Marker && it.id == id } as Marker?
      if (existingMarker != null) {
        existingMarker.position = latLng.toGeoPoint()
      } else if (activity?.isDestroyed == false) {
        /*
        There's a race condition where in the time it takes to create all the markers, the
        activity has been destroyed. Creating a Marker requires (for some reason) the `mapView`
        to be attached to a non-destroyed activity somehow, so we check before creating the marker
         */

        overlays.add(
            overlays.filterIsInstance<MyLocationNewOverlay>().indexOfFirst { true },
            Marker(this).apply {
              this.id = id
              position = latLng.toGeoPoint()
              infoWindow = null
              setOnMarkerClickListener { marker, _ ->
                onMarkerClicked(marker.id)
                true
              }
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            })
      }
      overlays
          .firstOrNull { it is Marker && it.id == id }
          ?.run { (this as Marker).icon = BitmapDrawable(resources, image) }
      invalidate()
    }
  }

  override fun removeMarkerFromMap(id: String) {
    mapView?.overlays?.removeAll { it is Marker && it.id == id }
  }

  override fun currentMarkersOnMap(): Set<String> =
      mapView?.overlays?.filterIsInstance<Marker>()?.map { it.id }?.toSet() ?: emptySet()

  override fun onResume() {
    super.onResume()
    mapView?.onResume()
    setMapStyle()
  }

  override fun onPause() {
    mapView?.onPause()
    super.onPause()
  }

  override fun onDetach() {
    mapView?.onDetach()
    super.onDetach()
  }

  /**
   * This gets fired on rotate. We need to trigger an onScroll event to reset the orientation
   * provider and re-draw the compass
   *
   * @param newConfig
   */
  override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
    super.onConfigurationChanged(newConfig)
    compassOrientationMapListener.onScroll(null)
  }

  override fun drawRegions(regions: Set<WaypointModel>) {
    if (preferences.showRegionsOnMap) {
      mapView?.run {
        Timber.d("Drawing ${regions.size} regions on map")
        overlays
            .filterIsInstance<Marker>()
            .filter { it.id.startsWith("regionmarker-") }
            .forEach(overlays::remove)
        overlays
            .filterIsInstance<Polygon>()
            .filter { it.id.startsWith("regionpolygon-") }
            .forEach(overlays::remove)

        regions
            .flatMap { region ->
              listOf(
                  Polygon(this).apply {
                    id = "regionpolygon-${region.id}"
                    points =
                        Polygon.pointsAsCircle(
                            region.getLocation().toLatLng().toGeoPoint(),
                            region.geofenceRadius.toDouble())
                    fillPaint.color = getRegionColor()
                    outlinePaint.strokeWidth = 1f
                    setOnClickListener { _, mapView, _ ->
                      mapView.overlays
                          .filterIsInstance<Marker>()
                          .first { it.id == "regionmarker-${region.id}" }
                          .showInfoWindow()
                      true
                    }
                  },
                  Marker(this).apply {
                    id = "regionmarker-${region.id}"
                    position = region.getLocation().toLatLng().toGeoPoint()
                    title = region.description
                    setInfoWindow(MarkerInfoWindow(R.layout.osm_region_bubble, this@run))
                  })
            }
            .let { overlays.addAll(0, it) }
      }
    }
  }

  override fun setMapLayerType(mapLayerStyle: MapLayerStyle) {
    when (mapLayerStyle) {
      MapLayerStyle.OpenStreetMapNormal ->
          binding.osmMapView.setTileSource(TileSourceFactory.MAPNIK)
      MapLayerStyle.OpenStreetMapWikimedia ->
          binding.osmMapView.setTileSource(TileSourceFactory.WIKIMEDIA)
      else -> Timber.w("Unsupported map layer type $mapLayerStyle")
    }
  }

  companion object {
    const val MIN_ZOOM_LEVEL: Double = 5.0
    const val MAX_ZOOM_LEVEL: Double = 21.0
  }
}
