package org.owntracks.android.support

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.messages.MessageLocation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceMetricsProvider @Inject internal constructor(@ApplicationContext private val context: Context) {
    val batteryLevel: Int
        get() {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
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
    @Suppress("DEPRECATION")
    val connectionType: String?
        get() {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                cm.run {
                    cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                        if (!hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                            return MessageLocation.CONN_TYPE_OFFLINE
                        }
                        if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            return MessageLocation.CONN_TYPE_MOBILE
                        }
                        if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            return MessageLocation.CONN_TYPE_WIFI
                        }
                    }
                }
                return null
            } else {

                val activeNetworkInfo = cm.activeNetworkInfo ?: return null
                if (!activeNetworkInfo.isConnected) {
                    return MessageLocation.CONN_TYPE_OFFLINE
                }
                return when (activeNetworkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> MessageLocation.CONN_TYPE_WIFI
                    ConnectivityManager.TYPE_MOBILE -> MessageLocation.CONN_TYPE_MOBILE
                    else -> null
                }
            }
        }
}