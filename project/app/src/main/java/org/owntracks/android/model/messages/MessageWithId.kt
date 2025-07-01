package org.owntracks.android.model.messages

import kotlin.random.Random
import kotlinx.serialization.SerialName

typealias MessageId = String

const val ZeroMessageId: MessageId = "0"

interface MessageWithId {
  @get:SerialName("_id") var messageId: MessageId
}

class MessageWithRandomId : MessageWithId {
  @OptIn(ExperimentalStdlibApi::class)
  override var messageId: MessageId = ByteArray(4).apply(Random::nextBytes).toHexString()
}
