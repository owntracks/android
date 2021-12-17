package org.owntracks.android.support

import org.owntracks.android.model.FusedContact
import java.util.*

class Events {
    abstract class E internal constructor() {
        val date: Date = Date()
    }

    class ModeChanged(val newModeId: Int) : E()
    class MonitoringChanged : E()
    class FusedContactAdded(val contact: FusedContact) : E()
    class FusedContactRemoved(val contact: FusedContact) : E()
}