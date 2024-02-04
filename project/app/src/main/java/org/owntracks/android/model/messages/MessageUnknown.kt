package org.owntracks.android.model.messages

object MessageUnknown : MessageBase() {
    const val TYPE = "unknown"
    override fun toString(): String = "[MessageUnknown]"
}
