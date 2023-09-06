package org.owntracks.android.data.repos

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.model.Contact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import timber.log.Timber

@Singleton
class MemoryContactsRepo @Inject constructor(
    private val contactsBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache,
    preferences: Preferences
) : ContactsRepo, Preferences.OnPreferenceChangeListener {

    private val contacts = mutableMapOf<String, Contact>()

    private val mutableRepoChangedEvent = MutableLiveData<ContactsRepoChange>()

    override val repoChangedEvent: LiveData<ContactsRepoChange> = mutableRepoChangedEvent
    override val all: Map<String, Contact>
        get() = contacts
    override fun getById(id: String): Contact? {
        return contacts[id]
    }

    @Synchronized
    private fun put(id: String, contact: Contact) {
        Timber.v("new contact allocated id=$id, tid=${contact.trackerId}")
        contacts[id] = contact
        mutableRepoChangedEvent.postValue(ContactsRepoChange.ContactAdded(contact))
    }

    @MainThread
    @Synchronized
    override fun clearAll() {
        contacts.clear()
        contactsBitmapAndNameMemoryCache.evictAll()
        mutableRepoChangedEvent.postValue(ContactsRepoChange.AllCleared)
    }

    @Synchronized
    override fun remove(id: String) {
        Timber.v("removing contact: $id")
        contacts.remove(id)?.run {
            mutableRepoChangedEvent.postValue(ContactsRepoChange.ContactRemoved(this))
        }
    }

    @Synchronized
    override fun update(id: String, messageCard: MessageCard) {
        Timber.v("Updating card for contact $id")
        getById(id)?.apply {
            this.messageCard = messageCard
            mutableRepoChangedEvent.postValue(ContactsRepoChange.ContactCardUpdated(this))
        } ?: run {
            Contact(id).apply { this.messageCard = messageCard }.also { put(id, it) }
        }
    }

    @Synchronized
    override fun update(id: String, messageLocation: MessageLocation) {
        Timber.v("Updating location for contact $id")
        getById(id)?.apply {
            // If timestamp of last location message is <= the new location message, skip update. We either received an old or already known message.
            if (setMessageLocation(messageLocation)) {
                mutableRepoChangedEvent.postValue(ContactsRepoChange.ContactLocationUpdated(this))
            }
        } ?: run {
            Contact(id).apply {
                setMessageLocation(messageLocation)
                // We may have seen this contact id before, and it may have been removed from the repo
                // Check the cache to see if we have a name
                contactsBitmapAndNameMemoryCache[id]?.also {
                    if (it is ContactBitmapAndName.CardBitmap && it.name != null) {
                        this.messageCard = MessageCard().apply { name = it.name }
                    }
                }
            }.also { put(id, it) }
        }
    }

    init {
        preferences.registerOnPreferenceChangedListener(this)
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        if (properties.contains("mode")) {
            clearAll()
        }
    }
}
