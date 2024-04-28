package org.owntracks.android.ui.map.osm

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.IOrientationProvider

class ClickableCompassOverlay(
    context: Context?,
    orientationProvider: IOrientationProvider?,
    mapView: MapView?
) : CompassOverlay(context, orientationProvider, mapView) {
  private fun getCompassRectangle(): Rect {
    val center = 35f * mScale
    return Rect(
        ((center - mCompassFrameCenterX).toInt()),
        ((center - mCompassFrameCenterY).toInt()),
        ((center + mCompassFrameCenterX).toInt()),
        ((center + mCompassFrameCenterY).toInt()))
  }

  /**
   * Tests to see if the given coordinates that are absolute to the display lie in a rectangle with
   * relative coordinates to the containing [MapView]
   *
   * @param rect Rectangle within the [mapView] to test
   * @param mapView The [MapView] containing the rectangle
   * @param x absolute X coordinate to test
   * @param y absolute Y coordinate to test
   * @return whether the X,Y coordinates are inside the [rect]
   */
  private fun hitTest(rect: Rect, mapView: MapView, x: Float, y: Float): Boolean =
      IntArray(2).apply(mapView::getLocationOnScreen).let { coords ->
        rect.apply { offset(coords[0], coords[1]) }.contains(x, y)
      }

  override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean =
      letBoth(e, mapView) { motionEvent, map ->
        if (hitTest(getCompassRectangle(), map, motionEvent.rawX, motionEvent.rawY)) {
          mapView?.run {
            setMapOrientation(0f, true)
            scrollX = 0 // Trigger "onScroll" to reset the compass
          }
          return true
        } else {
          return false
        }
      } ?: false

  // https://stackoverflow.com/a/35522422/352740
  private inline fun <T1 : Any, T2 : Any, R : Any> letBoth(
      p1: T1?,
      p2: T2?,
      block: (T1, T2) -> R?
  ): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
  }

  private fun Rect.contains(x: Float, y: Float): Boolean =
      x >= this.left && x <= this.right && y <= this.bottom && y >= this.top
}
