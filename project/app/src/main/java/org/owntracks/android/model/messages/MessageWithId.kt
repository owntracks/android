package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.random.Random

typealias MessageId = String

const val ZeroMessageId: MessageId = "0"

interface MessageWithId {
  @get:JsonProperty("_id") var messageId: MessageId
}

class MessageWithRandomId : MessageWithId {
  @OptIn(ExperimentalStdlibApi::class)
  override var messageId: MessageId = ByteArray(4).apply(Random::nextBytes).toHexString()
}
