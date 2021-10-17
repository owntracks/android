package org.owntracks.android.support.interfaces

interface OutgoingMessageProcessor {
    fun onCreateFromProcessor()
    fun onDestroy()

    @Throws(ConfigurationIncompleteException::class)
    fun checkConfigurationComplete()
}