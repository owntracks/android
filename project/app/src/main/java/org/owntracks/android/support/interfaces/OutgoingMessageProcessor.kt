package org.owntracks.android.support.interfaces

import org.owntracks.android.net.ConnectionConfiguration

interface OutgoingMessageProcessor {
    fun activate()
    fun deactivate()

    @Throws(ConfigurationIncompleteException::class)
    fun getEndpointConfiguration(): ConnectionConfiguration
}
