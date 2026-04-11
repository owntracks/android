package org.owntracks.android.support.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.ui.mixins.ServiceStarter
import timber.log.Timber

/**
 * Exported receiver that allows external apps (e.g. Tasker) to trigger location publishes and
 * change the monitoring mode, gated behind the [Preferences.allowIntentControl] preference.
 */
@AndroidEntryPoint
class ExternalIntentReceiver : BroadcastReceiver(), ServiceStarter by ServiceStarter.Impl() {
  @Inject lateinit var preferences: Preferences

  override fun onReceive(context: Context, intent: Intent) {
    if (!preferences.allowIntentControl) {
      Timber.w("Received external intent ${intent.action} but allowIntentControl is disabled")
      return
    }
    when (intent.action) {
      BackgroundService.INTENT_ACTION_SEND_LOCATION_USER,
      BackgroundService.INTENT_ACTION_CHANGE_MONITORING -> {
        Timber.d("Forwarding external intent ${intent.action} to BackgroundService")
        startService(context, intent.action, intent)
      }
      else -> Timber.w("Received unexpected action ${intent.action} in ExternalIntentReceiver")
    }
  }
}
