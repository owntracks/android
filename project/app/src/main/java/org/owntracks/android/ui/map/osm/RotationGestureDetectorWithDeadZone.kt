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

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.atan2
import org.osmdroid.views.MapView

/**
 * An osmdroid rotation gesture detector that only triggers a map rotation once the total rotation
 * is greater than a "dead zone". This reduces the rotation sensitivity for users making small
 * adjustments, and prevents accidental rotation when zooming.
 *
 * @property mListener
 * @constructor Create empty Rotation gesture detector with dead zone
 */
class RotationGestureDetectorWithDeadZone(private val mListener: RotationListener) {
  /**
   * heads up, this class is used internally by osmdroid, you're welcome to use but it the interface
   * [RotationListener] will not fire as expected. It is used internally by osmdroid. If you want to
   * listen for rotation changes on the [org.osmdroid.views.MapView] then use
   * [org.osmdroid.views.MapView.setMapListener] and check for [MapView.getMapOrientation] See
   * [https://github.com/osmdroid/osmdroid/issues/628](https://github.com/osmdroid/osmdroid/issues/628)
   */
  interface RotationListener {
    fun onRotate(deltaAngle: Float)
  }

  private val deadZone = 15f // Degrees
  private var isDead = true
  private var initialRotation = 0f
  private var inputOrientation: Float = 0f
  var isEnabled: Boolean = true

  fun onTouch(e: MotionEvent) {
    if (e.pointerCount != 2) return

    if (e.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
      inputOrientation = rotation(e)
      initialRotation = inputOrientation
    } else if (e.actionMasked == MotionEvent.ACTION_POINTER_UP) {
      isDead = true
    }

    val rotation: Float = rotation(e)
    val delta = rotation - inputOrientation

    // we have to allow detector to capture and store the new rotation to avoid UI jump when
    // user enables the overlay again
    if (isEnabled) {
      inputOrientation += delta
      if (abs(rotation - initialRotation) > deadZone && isDead) {
        isDead = false
      }
      if (!isDead) {
        mListener.onRotate(delta)
      }
    } else {
      inputOrientation = rotation
    }
  }

  companion object {
    private fun rotation(event: MotionEvent): Float {
      val deltaX = (event.getX(0) - event.getX(1)).toDouble()
      val deltaY = (event.getY(0) - event.getY(1)).toDouble()
      val radians = atan2(deltaY, deltaX)
      return Math.toDegrees(radians).toFloat()
    }
  }
}
