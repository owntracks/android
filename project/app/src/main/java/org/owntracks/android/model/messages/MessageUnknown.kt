package org.owntracks.android.model.messages

object MessageUnknown : MessageBase(), MessageWithId {
  override var messageId: MessageId = ZeroMessageId
  const val TYPE = "unknown"

  override fun toString(): String = "[MessageUnknown]"
}
