package org.owntracks.android.support

import android.content.Context
import org.owntracks.android.R
import org.owntracks.android.preferences.types.UnitsDisplay

data class Speed(val value: Int) {
  fun toMph(): Int = (value / KPH_PER_MPH).toInt()

  companion object {
    private const val KPH_PER_MPH = 1.60934

    @JvmStatic
    fun format(context: Context, speed: Speed, units: UnitsDisplay?): String =
        when (units) {
          UnitsDisplay.IMPERIAL ->
              context.getString(R.string.contactDetailsSpeedValueImperial, speed.toMph())
          else -> context.getString(R.string.contactDetailsSpeedValue, speed.value)
        }
  }
}
