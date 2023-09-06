package org.owntracks.android.data.repos

import androidx.lifecycle.LiveData
import org.owntracks.android.model.Contact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation

interface ContactsRepo {
    val all: Map<String, Contact>
    val repoChangedEvent: LiveData<ContactsRepoChange>
    fun getById(id: String): Contact?
    fun clearAll()
    fun remove(id: String)
    fun update(id: String, messageLocation: MessageLocation)
    fun update(id: String, messageCard: MessageCard)
}

sealed class ContactsRepoChange {
    data class ContactAdded(val contact: Contact) : ContactsRepoChange()
    data class ContactRemoved(val contact: Contact) : ContactsRepoChange()
    data class ContactLocationUpdated(val contact: Contact) : ContactsRepoChange()
    data class ContactCardUpdated(val contact: Contact) : ContactsRepoChange()
    object AllCleared : ContactsRepoChange()
}
