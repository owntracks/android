package org.owntracks.android.data.repos

import kotlinx.coroutines.flow.SharedFlow
import org.owntracks.android.model.Contact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition

/**
 * A storage for Contacts. This is populated on startup by whatever's in the upstream recorder, so we don't persist this
 * in anything other than a simple Map. On changes, we emit a [ContactsRepoChange] event.
 */
interface ContactsRepo {
    val all: Map<String, Contact>
    val repoChangedEvent: SharedFlow<ContactsRepoChange>
    fun getById(id: String): Contact?
    suspend fun clearAll()
    suspend fun remove(id: String)
    suspend fun update(id: String, messageLocation: MessageLocation)
    suspend fun update(id: String, messageTransition: MessageTransition)
    suspend fun update(id: String, messageCard: MessageCard)
}

sealed class ContactsRepoChange {
    data class ContactAdded(val contact: Contact) : ContactsRepoChange()
    data class ContactRemoved(val contact: Contact) : ContactsRepoChange()
    data class ContactLocationUpdated(val contact: Contact) : ContactsRepoChange()
    data class ContactCardUpdated(val contact: Contact) : ContactsRepoChange()
    data object AllCleared : ContactsRepoChange()
}
