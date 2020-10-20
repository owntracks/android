package org.owntracks.android.support

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.BatteryManager
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.injection.scopes.PerApplication
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.BatteryStatus
import javax.inject.Inject

@PerApplication
class DeviceMetricsProvider @Inject internal constructor(@param:AppContext private val context: Context) {
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
        return when(batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, 0)) {
            BatteryManager.BATTERY_STATUS_FULL ->  BatteryStatus.FULL
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.UNPLUGGED
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.UNKNOWN
            else -> BatteryStatus.UNKNOWN
        }
    }
    val connectionType: String?
        get() {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var activeNetwork: NetworkInfo
            if (cm.activeNetworkInfo.also { activeNetwork = it!! } != null) {
                if (!activeNetwork.isConnected) {
                    return MessageLocation.CONN_TYPE_OFFLINE
                } else if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                    return MessageLocation.CONN_TYPE_WIFI
                } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                    return MessageLocation.CONN_TYPE_MOBILE
                }
            }
            return null
        }
}