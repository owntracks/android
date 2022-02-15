package org.owntracks.android.support

import org.owntracks.android.data.WaypointModel
import org.owntracks.android.model.FusedContact
import java.util.*

class Events {
    abstract class E internal constructor() {
        val date: Date = Date()
    }

    class ModeChanged(val newModeId: Int) : E()
    class MonitoringChanged : E()
    class EndpointChanged : E()
    class ServiceStarted : E()
    class QueueChanged : E() {
        var newLength = 0
        fun withNewLength(length: Int): QueueChanged {
            newLength = length
            return this
        }
    }

    open class WaypointEvent internal constructor(val waypointModel: WaypointModel) : E()
    class WaypointAdded(m: WaypointModel) : WaypointEvent(m)
    class WaypointUpdated(m: WaypointModel) : WaypointEvent(m)
    class WaypointRemoved(m: WaypointModel) : WaypointEvent(m)
    class RestartApp : E()

    class WelcomeNextDoneButtonsEnableToggle(val nextEnabled: Boolean = true, val doneEnabled: Boolean = false) : E()
}