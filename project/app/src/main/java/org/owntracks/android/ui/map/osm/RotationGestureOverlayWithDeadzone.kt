/* Copied and adapted from <osmdroid> (https://github.com/osmdroid/osmdroid).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.owntracks.android.ui.map.osm

import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.IOverlayMenuProvider
import org.osmdroid.views.overlay.Overlay

/**
 * An osmdroid overlay that implements rotation gestures, but includes a dead zone to allow zooming
 * without rotating
 *
 * @property mapView
 * @constructor Create empty Rotation gesture overlay with dead zone
 */
class RotationGestureOverlayWithDeadZone(private var mapView: MapView) :
    Overlay(), RotationGestureDetectorWithDeadZone.RotationListener, IOverlayMenuProvider {
  private val rotationDetector = RotationGestureDetectorWithDeadZone(this)
  private var optionsMenuEnabled = false

  override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
    rotationDetector.onTouch(event)
    return super.onTouchEvent(event, mapView)
  }

  private var timeLastSet: Long = 0L
  private val deltaTime: Long = 25L
  private var currentAngle: Float = 0f

  override fun onRotate(deltaAngle: Float) {
    currentAngle += deltaAngle
    if (System.currentTimeMillis() - deltaTime > timeLastSet) {
      timeLastSet = System.currentTimeMillis()
      mapView.mapOrientation += currentAngle
    }
  }

  override fun onDetach(map: MapView) {
    super.onDetach(map)
  }

  override fun isOptionsMenuEnabled(): Boolean {
    return optionsMenuEnabled
  }

  override fun onCreateOptionsMenu(pMenu: Menu, pMenuIdOffset: Int, pMapView: MapView): Boolean {
    return true
  }

  override fun onOptionsItemSelected(
      menuItem: MenuItem,
      menuIdOffset: Int,
      mapView: MapView
  ): Boolean {
    return false
  }

  override fun onPrepareOptionsMenu(pMenu: Menu, pMenuIdOffset: Int, pMapView: MapView): Boolean {
    return false
  }

  override fun setOptionsMenuEnabled(enabled: Boolean) {
    // NOOP
  }

  override fun setEnabled(pEnabled: Boolean) {
    rotationDetector.isEnabled = pEnabled
    super.setEnabled(pEnabled)
  }
}
