package org.owntracks.android.net

import java.io.IOException
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor

abstract class MessageProcessorEndpoint internal constructor(val messageProcessor: MessageProcessor) :
    OutgoingMessageProcessor {
    fun onMessageReceived(message: MessageBase) {
        message.modeId = modeId!!
        messageProcessor.processIncomingMessage(onFinalizeMessage(message))
    }

    protected abstract fun onFinalizeMessage(message: MessageBase): MessageBase
    abstract val modeId: ConnectionMode?

    @Throws(
        ConfigurationIncompleteException::class,
        OutgoingMessageSendingException::class,
        IOException::class
    )
    abstract suspend fun sendMessage(message: MessageBase)
}

class OutgoingMessageSendingException internal constructor(e: Exception?) : Exception(e)
