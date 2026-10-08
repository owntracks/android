package org.owntracks.android.ui.map.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.owntracks.android.R
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.databinding.UiLocationPickerBinding
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.parseLatLng
import org.owntracks.android.location.toGeoPoint
import org.owntracks.android.location.toLatLng
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.map.osm.configureOsmdroid

/**
 * Lets the user pick a location by moving a map under a pin, or typing in its coordinates. Uses
 * OpenStreetMap, so that it works the same in every flavour.
 */
@AndroidEntryPoint
class LocationPickerActivity : AppCompatActivity() {
  @Inject lateinit var preferences: Preferences

  /**
   * The app's own location client. Got through an entry point because the oss flavour also binds a
   * different one for activities, which would make injecting it here ambiguous.
   */
  private val locationProviderClient by lazy {
    EntryPoints.get(applicationContext, LocationPickerEntryPoint::class.java)
        .locationProviderClient()
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  internal interface LocationPickerEntryPoint {
    fun locationProviderClient(): LocationProviderClient
  }

  @Inject lateinit var locationRepo: LocationRepo

  @Inject lateinit var requirementsChecker: RequirementsChecker

  @Inject @CoroutineScopes.IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

  private lateinit var binding: UiLocationPickerBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    configureOsmdroid(this)
    binding =
        DataBindingUtil.setContentView<UiLocationPickerBinding>(this, R.layout.ui_location_picker)
            .apply {
              setSupportActionBar(appbar.toolbar)
              val controlsPadding = resources.getDimensionPixelSize(R.dimen.default_spacing)
              ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
                val insets =
                    windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                    )
                appbar.root.updatePadding(top = insets.top)
                controls.updatePadding(bottom = controlsPadding + insets.bottom)
                WindowInsetsCompat.CONSUMED
              }
              mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                minZoomLevel = MIN_ZOOM_LEVEL
                maxZoomLevel = MAX_ZOOM_LEVEL
                tilesScaleFactor = preferences.osmTileScaleFactor
                controller.setZoom(MIN_ZOOM_LEVEL)
                addMapListener(
                    object : MapListener {
                      override fun onScroll(event: ScrollEvent?): Boolean {
                        showMapCenter()
                        return false
                      }

                      override fun onZoom(event: ZoomEvent?): Boolean = false
                    }
                )
              }
              coordinates.doAfterTextChanged { text ->
                // Only follow the text while the user is typing it, not when the map is setting it
                if (coordinates.hasFocus()) {
                  val latLng = parseLatLng(text.toString())
                  coordinatesLayout.error =
                      if (latLng == null) getString(R.string.locationPickerCoordinatesInvalid)
                      else null
                  save.isEnabled = latLng != null
                  latLng?.run(::centerOn)
                }
              }
              coordinates.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) stopEditingCoordinates()
                false
              }
              // Moving the map hands control of the coordinates back from the text field to the map
              @Suppress("ClickableViewAccessibility")
              mapView.setOnTouchListener { _, _ ->
                stopEditingCoordinates()
                false
              }
              myLocation.setOnClickListener {
                stopEditingCoordinates()
                centerOnCurrentLocation()
              }
              clear.isVisible = intent.getBooleanExtra(EXTRA_CAN_CLEAR, false)
              clear.setOnClickListener {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_CLEARED, true))
                finish()
              }
              save.setOnClickListener {
                val center = mapView.mapCenter
                setResult(
                    Activity.RESULT_OK,
                    Intent()
                        .putExtra(EXTRA_LATITUDE, center.latitude)
                        .putExtra(EXTRA_LONGITUDE, center.longitude),
                )
                finish()
              }
            }
    supportActionBar?.run {
      setDisplayShowHomeEnabled(true)
      setDisplayHomeAsUpEnabled(true)
    }
    if (savedInstanceState == null) {
      intent.latLng()?.run(::centerOn) ?: centerOnCurrentLocation()
    } else {
      // The map doesn't keep where it was itself, e.g. when the screen rotates
      binding.mapView.controller.setZoom(
          savedInstanceState.getDouble(STATE_ZOOM, PICKING_ZOOM_LEVEL)
      )
      savedInstanceState.latLng()?.run { binding.mapView.controller.setCenter(toGeoPoint()) }
      showMapCenter()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    binding.mapView.run {
      outState.putDouble(EXTRA_LATITUDE, mapCenter.latitude)
      outState.putDouble(EXTRA_LONGITUDE, mapCenter.longitude)
      outState.putDouble(STATE_ZOOM, zoomLevelDouble)
    }
  }

  private fun centerOn(latLng: LatLng) {
    binding.mapView.controller.run {
      if (binding.mapView.zoomLevelDouble < PICKING_ZOOM_LEVEL) setZoom(PICKING_ZOOM_LEVEL)
      setCenter(latLng.toGeoPoint())
    }
    showMapCenter()
  }

  /** The device's own location, which isn't necessarily the one we last published */
  private fun centerOnCurrentLocation() {
    lifecycleScope.launch {
      val location =
          if (requirementsChecker.hasLocationPermissions()) {
            withContext(ioDispatcher) { locationProviderClient.getLastLocation() }
          } else {
            null
          } ?: locationRepo.currentPublishedLocation.value
      location?.toLatLng()?.run(::centerOn)
    }
  }

  private fun stopEditingCoordinates() {
    binding.coordinates.run {
      if (hasFocus()) {
        clearFocus()
        WindowCompat.getInsetsController(window, this).hide(WindowInsetsCompat.Type.ime())
        showMapCenter()
      }
    }
  }

  private fun showMapCenter() {
    binding.run {
      if (!coordinates.hasFocus()) {
        coordinates.setText(mapView.mapCenter.run { LatLng(latitude, longitude) }.toDisplayString())
        coordinatesLayout.error = null
        save.isEnabled = true
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return true
  }

  override fun onResume() {
    super.onResume()
    binding.mapView.onResume()
  }

  override fun onPause() {
    binding.mapView.onPause()
    super.onPause()
  }

  /** What was picked, or that the location should be cleared */
  sealed interface Result {
    data class Picked(val latLng: LatLng) : Result

    data object Cleared : Result
  }

  /**
   * Picks a location, starting the map at [Input.initial], or the current location if there isn't
   * one. Gives null if the user backs out.
   */
  class PickLocation : ActivityResultContract<PickLocation.Input, Result?>() {
    data class Input(val initial: LatLng?, val canClear: Boolean)

    override fun createIntent(context: Context, input: Input): Intent =
        Intent(context, LocationPickerActivity::class.java).apply {
          input.initial?.run {
            putExtra(EXTRA_LATITUDE, latitude.value)
            putExtra(EXTRA_LONGITUDE, longitude.value)
          }
          putExtra(EXTRA_CAN_CLEAR, input.canClear)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Result? =
        when {
          resultCode != Activity.RESULT_OK -> null
          intent?.getBooleanExtra(EXTRA_CLEARED, false) == true -> Result.Cleared
          else -> intent?.latLng()?.let(Result::Picked)
        }
  }

  companion object {
    private const val EXTRA_LATITUDE = "latitude"
    private const val EXTRA_LONGITUDE = "longitude"
    private const val EXTRA_CAN_CLEAR = "canClear"
    private const val EXTRA_CLEARED = "cleared"
    private const val STATE_ZOOM = "zoom"
    private const val MIN_ZOOM_LEVEL = 3.0
    private const val MAX_ZOOM_LEVEL = 21.0
    private const val PICKING_ZOOM_LEVEL = 17.0

    private fun Intent.latLng(): LatLng? = extras?.latLng()

    private fun Bundle.latLng(): LatLng? =
        if (containsKey(EXTRA_LATITUDE) && containsKey(EXTRA_LONGITUDE)) {
          LatLng(getDouble(EXTRA_LATITUDE), getDouble(EXTRA_LONGITUDE))
        } else {
          null
        }
  }
}
