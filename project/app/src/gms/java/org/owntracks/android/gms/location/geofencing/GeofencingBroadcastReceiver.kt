package org.owntracks.android.gms.location.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.owntracks.android.services.BackgroundService.Companion.INTENT_ACTION_SEND_EVENT_CIRCULAR
import org.owntracks.android.ui.mixins.ServiceStarter

/**
 * Receives geofencing events from the framework, then invokes the service with the intent so that
 * we can notify the user
 *
 * @constructor Create empty Geofencing broadcast receiver
 */
class GeofencingBroadcastReceiver : BroadcastReceiver(), ServiceStarter by ServiceStarter.Impl() {
  override fun onReceive(context: Context, intent: Intent) {
    startService(context, INTENT_ACTION_SEND_EVENT_CIRCULAR, intent)
  }
}
