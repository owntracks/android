package org.owntracks.android.net.mqtt

import android.os.SystemClock
import org.eclipse.paho.client.mqttv3.internal.HighResolutionTimer

/**
 * An android `HighResolutionTimer` implementation that includes the time spent asleep.
 */
class AndroidHighResolutionTimer : HighResolutionTimer {
    override fun nanoTime(): Long = SystemClock.elapsedRealtimeNanos()
}
