package org.owntracks.android.support.interfaces

import org.owntracks.android.services.ConnectionConfiguration

interface OutgoingMessageProcessor {
    fun activate()
    fun deactivate()

    @Throws(ConfigurationIncompleteException::class)
    fun getEndpointConfiguration(): ConnectionConfiguration
}
