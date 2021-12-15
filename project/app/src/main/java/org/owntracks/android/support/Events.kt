package org.owntracks.android.support

import java.util.*

class Events {
    abstract class E internal constructor() {
        val date: Date = Date()
    }

    class ModeChanged(val newModeId: Int) : E()
}