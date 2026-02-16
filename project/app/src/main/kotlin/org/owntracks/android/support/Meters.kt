package org.owntracks.android.support

import android.content.Context
import org.owntracks.android.R
import org.owntracks.android.preferences.types.UnitsDisplay

data class Meters(val value: Double) {
  constructor(value: Int) : this(value.toDouble())

  constructor(value: Float) : this(value.toDouble())

  fun toFeet(): Double = value * METERS_TO_FEET

  fun toMiles(): Double = value / METERS_PER_MILE

  companion object {
    private const val METERS_TO_FEET = 3.28084
    private const val METERS_PER_MILE = 1609.34

    @JvmStatic
    fun formatAsAccuracy(context: Context, meters: Meters, units: UnitsDisplay?): String =
        when (units) {
          UnitsDisplay.IMPERIAL ->
              context.getString(
                  R.string.contactDetailsAccuracyValueImperial, meters.toFeet().toInt())
          else -> context.getString(R.string.contactDetailsAccuracyValue, meters.value.toInt())
        }

    @JvmStatic
    fun formatAsAltitude(context: Context, meters: Meters, units: UnitsDisplay?): String =
        when (units) {
          UnitsDisplay.IMPERIAL ->
              context.getString(
                  R.string.contactDetailsAltitudeValueImperial, meters.toFeet().toInt())
          else -> context.getString(R.string.contactDetailsAltitudeValue, meters.value.toInt())
        }

    @JvmStatic
    fun formatAsDistance(context: Context, meters: Meters, units: UnitsDisplay?): String =
        when (units) {
          UnitsDisplay.IMPERIAL -> {
            if (meters.value > METERS_PER_MILE) {
              context.getString(
                  R.string.contactDetailsDistanceValue,
                  meters.toMiles().toFloat(),
                  context.getString(R.string.contactDetailsDistanceUnitMiles))
            } else {
              context.getString(
                  R.string.contactDetailsDistanceValue,
                  meters.toFeet().toFloat(),
                  context.getString(R.string.contactDetailsDistanceUnitFeet))
            }
          }
          else -> {
            if (meters.value > 1000.0) {
              context.getString(
                  R.string.contactDetailsDistanceValue,
                  (meters.value / 1000.0).toFloat(),
                  context.getString(R.string.contactDetailsDistanceUnitKilometres))
            } else {
              context.getString(
                  R.string.contactDetailsDistanceValue,
                  meters.value.toFloat(),
                  context.getString(R.string.contactDetailsDistanceUnitMeters))
            }
          }
        }
  }
}
