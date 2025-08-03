package org.owntracks.android.support.receiver

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.services.BackgroundService
import timber.log.Timber

@AndroidEntryPoint
class StartBackgroundServiceReceiver : BroadcastReceiver() {
  companion object {
    fun enable(context: Context) {
      val receiver = ComponentName(context, BroadcastReceiver::class.java)
      context.packageManager.setComponentEnabledSetting(
          receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }
  }

  @Inject lateinit var preferences: Preferences

  override fun onReceive(context: Context, intent: Intent) {
    if ((Intent.ACTION_MY_PACKAGE_REPLACED == intent.action ||
        Intent.ACTION_BOOT_COMPLETED == intent.action) && preferences.autostartOnBoot) {
      Timber.v("android.intent.action.BOOT_COMPLETED received")
      val startIntent = Intent(context, BackgroundService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Timber.v("running startForegroundService")
        startIntent.action = intent.action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          try {
            context.startForegroundService(startIntent)
          } catch (e: ForegroundServiceStartNotAllowedException) {
            Timber.e(
                e,
                "Unable to start foreground service, because Android has prevented it. " +
                    "This should not happen if intent action is ${Intent.ACTION_MY_PACKAGE_REPLACED} or " +
                    "${Intent.ACTION_BOOT_COMPLETED}. intent action was ${intent.action}")
          }
        } else {
          context.startForegroundService(startIntent)
        }
      } else {
        Timber.v("running legacy startService")
        context.startService(startIntent)
      }
    }
  }
}
