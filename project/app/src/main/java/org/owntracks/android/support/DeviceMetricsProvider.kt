package org.owntracks.android.support

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants.DISABLED
import com.google.common.util.concurrent.ListenableFuture
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

    val powerSave: Int
      get() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // return 0 if no power save
        return (if (powerManager.isPowerSaveMode) 1 else 0)
      }

    val batteryOptimizations: Int
      get() {
        // return 0 if no battery optimizations
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
          val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
          if (powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
            0
          } else {
            1
          }
        } else {
          0
        }
      }

    val appHibernation: Int
      get() {
        val future: ListenableFuture<Int> =
          PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
        // return 0 if no app hibernation
        return if (future.get() == DISABLED) {
          0
        } else {
          1
        }
      }

    val locationPermission: Int
      get() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
          var resultBack = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
          )
          var resultFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
          )
          var resultCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
          )
          /* create a response of:
          0 = Background location, fine precision
          -1 = Background location, coarse precision
          -2 = Foreground location, fine precision
          -3 = Foreground location, coarse precision
          -4 = Disabled
          */
          return (2*resultBack + resultFine + resultCoarse)
        } else {
          return (0)
        }
      }
}
