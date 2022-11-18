package org.owntracks.android.support

import org.owntracks.android.data.WaypointModel
import org.owntracks.android.preferences.types.ConnectionMode
import java.util.*

class Events {
    abstract class E internal constructor() {
        val date: Date = Date()
    }

    class MonitoringChanged : E()
    class EndpointChanged : E()

    open class WaypointEvent internal constructor(val waypointModel: WaypointModel) : E()
    class WaypointAdded(m: WaypointModel) : WaypointEvent(m)
    class WaypointUpdated(m: WaypointModel) : WaypointEvent(m)
    class WaypointRemoved(m: WaypointModel) : WaypointEvent(m)

    class WelcomeNextDoneButtonsEnableToggle(val nextEnabled: Boolean = true, val doneEnabled: Boolean = false) : E()
}
