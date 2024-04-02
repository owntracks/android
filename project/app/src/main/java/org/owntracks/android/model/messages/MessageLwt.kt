package org.owntracks.android.model.messages

object MessageLwt : MessageBase(), MessageWithId {
    const val TYPE = "lwt"
    override var messageId: MessageId = ZeroMessageId

    override fun toString(): String = "[MessageLwt]"
}
