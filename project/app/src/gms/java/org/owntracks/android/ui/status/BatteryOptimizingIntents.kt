package org.owntracks.android.ui.status

import android.content.Context
import android.content.Intent
import android.provider.Settings

@Suppress("UNUSED_PARAMETER")
class BatteryOptimizingIntents(_context: Context) {
  val settingsIntent: Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

  /*
  The Google Play Store rules don't allow you to link directly to the dialog that allows a user
  to disable battery optimizations for your app. They effectively make you go through the longer
  path, via the main settings. So in that case, we re-use the generic settings intent. See also
  the lack of asking for `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` in the
  android manifest for the gms flavor.
  */
  val directPackageIntent = settingsIntent
}
