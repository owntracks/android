package org.owntracks.android.support.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.support.Preferences
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class StartBackgroundServiceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: Preferences

    override fun onReceive(context: Context, intent: Intent) {
        if (("android.intent.action.MY_PACKAGE_REPLACED" == intent.action ||
                    "android.intent.action.BOOT_COMPLETED" == intent.action)
            && preferences.autostartOnBoot
        ) {
            Timber.v("android.intent.action.BOOT_COMPLETED received")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Timber.v("running startForegroundService")
                val startIntent = Intent(context, BackgroundService::class.java)
                startIntent.action = intent.action
                context.startForegroundService(startIntent)
            } else {
                Timber.v("running legacy startService")
                context.startService(Intent(context, BackgroundService::class.java))
            }
        }
    }
}