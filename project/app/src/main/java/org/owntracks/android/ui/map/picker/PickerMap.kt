package org.owntracks.android.ui.map.picker

import android.os.Bundle
import android.view.View
import org.owntracks.android.location.LatLng

/**
 * The map that a location is picked on, which is the same kind of map that the user has chosen for
 * the main map. Its lifecycle methods need calling from the activity showing it.
 */
interface PickerMap {
  val view: View

  /** The location in the middle of the map */
  val center: LatLng

  val zoom: Double

  fun moveTo(latLng: LatLng, zoom: Double)

  /** [onMove] is called whenever the map moves, whether by the user or by [moveTo] */
  fun setOnMoveListener(onMove: () -> Unit)

  /** [onUserMove] is called when the user starts moving the map */
  fun setOnUserMoveListener(onUserMove: () -> Unit)

  fun onCreate(savedInstanceState: Bundle?) {}

  fun onResume()

  fun onPause()

  fun onDestroy() {}

  fun onSaveInstanceState(outState: Bundle) {}

  companion object {
    const val MIN_ZOOM_LEVEL = 3.0
    const val MAX_ZOOM_LEVEL = 21.0
  }
}
