package org.owntracks.android.ui.status

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
import androidx.core.net.toUri

class BatteryOptimizingIntents(context: Context) {
  val settingsIntent: Intent = Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
  val directPackageIntent =
      Intent(
          Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
          "package:${context.packageName}".toUri())
}
