package org.owntracks.android.support

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.BatteryManager
import org.owntracks.android.injection.scopes.PerApplication
import org.owntracks.android.messages.MessageLocation
import javax.inject.Inject

@PerApplication
class DeviceMetricsProvider @Inject internal constructor(@param:AppContext private val context: Context) {
    val batteryLevel: Int
        get() {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        }
    val connectionType: String?
        get() {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var activeNetwork: NetworkInfo
            if (cm != null && cm.activeNetworkInfo.also { activeNetwork = it!! } != null) {
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