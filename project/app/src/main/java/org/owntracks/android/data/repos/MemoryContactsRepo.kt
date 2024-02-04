package org.owntracks.android.data.repos

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.owntracks.android.model.Contact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import timber.log.Timber

@Singleton
class MemoryContactsRepo @Inject constructor(
    private val contactsBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache,
) : ContactsRepo {

    private val contacts = mutableMapOf<String, Contact>()

    private val mutableRepoChangedEvent = MutableSharedFlow<ContactsRepoChange>()

    override val repoChangedEvent: SharedFlow<ContactsRepoChange> = mutableRepoChangedEvent
    override val all: Map<String, Contact>
        get() = contacts
    override fun getById(id: String): Contact? {
        return contacts[id]
    }

    private suspend fun put(id: String, contact: Contact) {
        synchronized(contacts) {
            Timber.v("new contact allocated id=$id, tid=${contact.trackerId}")
            contacts[id] = contact
        }
        mutableRepoChangedEvent.emit(ContactsRepoChange.ContactAdded(contact))
    }

    override suspend fun clearAll() {
        Timber.i("Clearing all contacts")
        contacts.clear()
        contactsBitmapAndNameMemoryCache.evictAll()
        mutableRepoChangedEvent.emit(ContactsRepoChange.AllCleared)
    }

    override suspend fun remove(id: String) {
        Timber.v("removing contact: $id")
        contacts.remove(id)?.run {
            mutableRepoChangedEvent.emit(ContactsRepoChange.ContactRemoved(this))
        }
    }

    override suspend fun update(id: String, messageCard: MessageCard) {
        getById(id)?.apply {
            this.setMessageCard(messageCard)
            mutableRepoChangedEvent.emit(ContactsRepoChange.ContactCardUpdated(this))
        } ?: run {
            Contact(id).apply { setMessageCard(messageCard) }.also { put(id, it) }
        }
    }

    override suspend fun update(id: String, messageLocation: MessageLocation) {
        Timber.v("Updating location for contact $id")
        getById(id)?.apply {
            // If timestamp of last location message is <= the new location message, skip update. We either received an old or already known message.
            if (setMessageLocation(messageLocation)) {
                mutableRepoChangedEvent.emit(ContactsRepoChange.ContactLocationUpdated(this))
            }
        } ?: run {
            Contact(id).apply {
                setMessageLocation(messageLocation)
                // We may have seen this contact id before, and it may have been removed from the repo
                // Check the cache to see if we have a name
                contactsBitmapAndNameMemoryCache[id]?.also {
                    if (it is ContactBitmapAndName.CardBitmap && it.name != null) {
                        setMessageCard(MessageCard().apply { name = it.name })
                    }
                }
            }.also { put(id, it) }
        }
    }
}
