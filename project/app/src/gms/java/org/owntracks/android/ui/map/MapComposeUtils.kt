package org.owntracks.android.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import org.owntracks.android.R
import org.owntracks.android.ui.map.osm.OSMMapFragment

private const val GOOGLE_MIN_ZOOM = 4.0
private const val GOOGLE_MAX_ZOOM = 20.0

/** Convert a Bitmap to a BitmapDescriptor for use with Google Maps Compose markers. */
fun Bitmap.toBitmapDescriptor(): BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(this)

/**
 * Converts standard (OSM) zoom to Google Maps zoom level. Simple linear conversion.
 *
 * @param inputZoom Zoom level from standard (OSM)
 * @return Equivalent zoom level on Google Maps
 */
fun convertStandardZoomToGoogleZoom(inputZoom: Double): Double =
    linearConversion(
        OSMMapFragment.MIN_ZOOM_LEVEL..OSMMapFragment.MAX_ZOOM_LEVEL,
        GOOGLE_MIN_ZOOM..GOOGLE_MAX_ZOOM,
        inputZoom.coerceIn(OSMMapFragment.MIN_ZOOM_LEVEL, OSMMapFragment.MAX_ZOOM_LEVEL))

/**
 * Converts Google Maps zoom to Standard (OSM) zoom level. Simple linear conversion.
 *
 * @param inputZoom Zoom level from Google Maps
 * @return Equivalent zoom level on Standard (OSM)
 */
fun convertGoogleZoomToStandardZoom(inputZoom: Double): Double =
    linearConversion(
        GOOGLE_MIN_ZOOM..GOOGLE_MAX_ZOOM,
        OSMMapFragment.MIN_ZOOM_LEVEL..OSMMapFragment.MAX_ZOOM_LEVEL,
        inputZoom.coerceIn(GOOGLE_MIN_ZOOM, GOOGLE_MAX_ZOOM))

/**
 * Linear conversion of a point in a range to the equivalent point in another range.
 *
 * @param fromRange Starting range the given point is in
 * @param toRange Range to translate the point to
 * @param point point in the starting range
 * @return a value that's at the same location in [toRange] as [point] is in [fromRange]
 */
private fun linearConversion(
    fromRange: ClosedRange<Double>,
    toRange: ClosedRange<Double>,
    point: Double
): Double {
  return ((point - fromRange.start) / (fromRange.endInclusive - fromRange.start)) *
      (toRange.endInclusive - toRange.start) + toRange.start
}

/**
 * Convert standard rotation to google bearing. OSM uses a "map rotation" concept to represent how
 * the map is oriented, whereas google uses the "bearing". These are not the same thing, so this
 * converts from a rotation to a bearing and back again (because it's reversible).
 *
 * @param input rotation or bearing value
 * @return an equivalent bearing or rotation
 */
fun convertBetweenStandardRotationAndBearing(input: Float): Float = -input % 360

/**
 * Remembers the map style options based on dark theme preference. Returns null for light theme,
 * and the night theme style for dark theme.
 */
@Composable
fun rememberMapStyleOptions(): MapStyleOptions? {
  val context = LocalContext.current
  val isDarkTheme = isSystemInDarkTheme()

  return remember(isDarkTheme) {
    if (isDarkTheme) {
      MapStyleOptions.loadRawResourceStyle(context, R.raw.google_maps_night_theme)
    } else {
      null
    }
  }
}

/** Gets the region color from the current theme. */
fun getRegionColor(context: Context): Int {
  val typedValue = TypedValue()
  context.theme.resolveAttribute(R.attr.colorRegion, typedValue, true)
  return typedValue.data
}

/** Gets the region color from the current theme as a Compose Color. */
@Composable
fun rememberRegionColor(): Color {
  val context = LocalContext.current
  return remember { Color(getRegionColor(context)) }
}
