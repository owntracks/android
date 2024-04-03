package org.owntracks.android.support

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.messages.MessageLocation
import timber.log.Timber

@Singleton
class DeviceMetricsProvider
@Inject
internal constructor(@ApplicationContext private val context: Context) {
  val batteryLevel: Int?
    get() {
      return context
          .registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
          ?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    }

  val batteryStatus: BatteryStatus
    get() {
      val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryStatus = context.registerReceiver(null, intentFilter)
      return when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, 0)) {
        BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
        BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.UNPLUGGED
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.UNKNOWN
        else -> BatteryStatus.UNKNOWN
      }
    }

  val connectionType: String
    get() {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

      return cm.run {
        try {
          cm.getNetworkCapabilities(cm.activeNetwork)?.run {
            if (hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
              if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                MessageLocation.CONN_TYPE_WIFI
              } else {
                MessageLocation.CONN_TYPE_MOBILE
              }
            } else {
              MessageLocation.CONN_TYPE_OFFLINE
            }
          }
          // Android bug: https://issuetracker.google.com/issues/175055271
          // ConnectivityManager::getNetworkCapabilities apparently throws a SecurityException
        } catch (e: SecurityException) {
          Timber.e(e, "Exception fetching NetworkCapabilities")
          null
        }
      } ?: MessageLocation.CONN_TYPE_OFFLINE
    }
}
