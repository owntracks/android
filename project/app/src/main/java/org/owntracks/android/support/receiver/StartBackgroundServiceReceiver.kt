package org.owntracks.android.support.receiver

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.android.DaggerBroadcastReceiver
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.support.Preferences
import timber.log.Timber
import javax.inject.Inject

class StartBackgroundServiceReceiver : DaggerBroadcastReceiver() {
    @JvmField
    @Inject
    var preferences: Preferences? = null
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (("android.intent.action.MY_PACKAGE_REPLACED" == intent.action ||
                        "android.intent.action.BOOT_COMPLETED" == intent.action)
                && preferences!!.autostartOnBoot) {
            Timber.v("android.intent.action.BOOT_COMPLETED received")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Timber.v("running startForegroundService")
                context.startForegroundService(Intent(context, BackgroundService::class.java))
            } else {
                Timber.v("running legacy startService")
                context.startService(Intent(context, BackgroundService::class.java))
            }
        }
    }
}