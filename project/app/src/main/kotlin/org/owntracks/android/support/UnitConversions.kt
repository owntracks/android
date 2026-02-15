package org.owntracks.android.support

import android.content.Context
import org.owntracks.android.R
import org.owntracks.android.preferences.types.UnitsDisplay

object UnitConversions {
  private const val METERS_TO_FEET = 3.28084
  private const val KPH_TO_MPH = 1.60934
  private const val METERS_TO_MILES = 1609.34

  @JvmStatic
  fun formatAccuracy(context: Context, accuracyMeters: Int, units: UnitsDisplay?): String =
      when (units) {
        UnitsDisplay.IMPERIAL ->
            context.getString(
                R.string.contactDetailsAccuracyValueImperial,
                (accuracyMeters * METERS_TO_FEET).toInt())
        else -> context.getString(R.string.contactDetailsAccuracyValue, accuracyMeters)
      }

  @JvmStatic
  fun formatAltitude(context: Context, altitudeMeters: Int, units: UnitsDisplay?): String =
      when (units) {
        UnitsDisplay.IMPERIAL ->
            context.getString(
                R.string.contactDetailsAltitudeValueImperial,
                (altitudeMeters * METERS_TO_FEET).toInt())
        else -> context.getString(R.string.contactDetailsAltitudeValue, altitudeMeters)
      }

  @JvmStatic
  fun formatSpeed(context: Context, velocityKph: Int, units: UnitsDisplay?): String =
      when (units) {
        UnitsDisplay.IMPERIAL ->
            context.getString(
                R.string.contactDetailsSpeedValueImperial, (velocityKph / KPH_TO_MPH).toInt())
        else -> context.getString(R.string.contactDetailsSpeedValue, velocityKph)
      }

  @JvmStatic
  fun formatDistance(context: Context, distanceMeters: Float, units: UnitsDisplay?): String =
      when (units) {
        UnitsDisplay.IMPERIAL -> {
          if (distanceMeters > METERS_TO_MILES) {
            context.getString(
                R.string.contactDetailsDistanceValue,
                distanceMeters / METERS_TO_MILES.toFloat(),
                context.getString(R.string.contactDetailsDistanceUnitMiles))
          } else {
            context.getString(
                R.string.contactDetailsDistanceValue,
                distanceMeters * METERS_TO_FEET.toFloat(),
                context.getString(R.string.contactDetailsDistanceUnitFeet))
          }
        }
        else -> {
          if (distanceMeters > 1000f) {
            context.getString(
                R.string.contactDetailsDistanceValue,
                distanceMeters / 1000f,
                context.getString(R.string.contactDetailsDistanceUnitKilometres))
          } else {
            context.getString(
                R.string.contactDetailsDistanceValue,
                distanceMeters,
                context.getString(R.string.contactDetailsDistanceUnitMeters))
          }
        }
      }
}
