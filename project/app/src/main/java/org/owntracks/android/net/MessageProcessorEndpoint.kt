package org.owntracks.android.net

import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor

abstract class MessageProcessorEndpoint
internal constructor(val messageProcessor: MessageProcessor) : OutgoingMessageProcessor {
  fun onMessageReceived(message: MessageBase) {
    message.modeId = modeId!!
    messageProcessor.processIncomingMessage(onFinalizeMessage(message))
  }

  protected abstract fun onFinalizeMessage(message: MessageBase): MessageBase

  abstract val modeId: ConnectionMode?

  abstract suspend fun sendMessage(message: MessageBase): Result<Unit>

  class NotReadyException : Exception()

  class OutgoingMessageSendingException internal constructor(e: Exception?) : Exception(e)
}
