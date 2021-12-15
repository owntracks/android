package org.owntracks.android.data.repos

import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import org.owntracks.android.support.Preferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryContactsRepo @Inject constructor(
        preferences: Preferences,
        private val contactsBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache
) : ContactsRepo, SharedPreferences.OnSharedPreferenceChangeListener {

    private val contacts = mutableMapOf<String, FusedContact>()
    override val all = MutableLiveData(contacts)
    override val lastUpdated = MutableLiveData<ContactUpdatedEvent?>(null)

    override fun getById(id: String): FusedContact? {
        return contacts[id]
    }

    @Synchronized
    private fun put(id: String, contact: FusedContact) {
        Timber.v("new contact allocated id:%s, tid:%s", id, contact.trackerId)
        contacts[id] = contact
        all.postValue(contacts)
        lastUpdated.postValue(ContactUpdatedEvent(contact, false))
    }

    @MainThread
    @Synchronized
    override fun clearAll() {
        contacts.clear()
        contactsBitmapAndNameMemoryCache.evictAll()
        all.postValue(contacts)
        lastUpdated.postValue(null)
    }

    @Synchronized
    override fun remove(id: String) {
        Timber.v("removing contact: %s", id)
        contacts.remove(id)?.apply { lastUpdated.postValue(ContactUpdatedEvent(this, true)) }
        all.postValue(contacts)

    }

    @Synchronized
    override fun update(id: String, messageCard: MessageCard) {
        var contact = getById(id)
        if (contact != null) {
            contact.messageCard = messageCard
            contactsBitmapAndNameMemoryCache.put(
                    contact.id,
                    ContactBitmapAndName.CardBitmap(messageCard.name, null)
            )
            lastUpdated.postValue(ContactUpdatedEvent(contact, false))
        } else {
            contact = FusedContact(id)
            contact.messageCard = messageCard
            contactsBitmapAndNameMemoryCache.put(
                    contact.id,
                    ContactBitmapAndName.CardBitmap(messageCard.name, null)
            )
            put(id, contact)
        }
    }

    @Synchronized
    override fun update(id: String, messageLocation: MessageLocation) {
        Timber.d("Updating contacts repo with cohtact $id with a location message $messageLocation")
        var fusedContact = getById(id)
        if (fusedContact != null) {
            Timber.d("Contact $id already exists")
            // If timestamp of last location message is <= the new location message, skip update. We either received an old or already known message.
            if (fusedContact.setMessageLocation(messageLocation)) {
                Timber.d("Location is new for contact $id. Updating")
                lastUpdated.postValue(ContactUpdatedEvent(fusedContact, false))
                all.postValue(contacts)
            }
        } else {
            Timber.d("Contact $id is new")
            fusedContact = FusedContact(id).apply {
                setMessageLocation(messageLocation)
                // We may have seen this contact id before, and it may have been removed from the repo
                // Check the cache to see if we have a name
                contactsBitmapAndNameMemoryCache[id]?.also {
                    if (it is ContactBitmapAndName.CardBitmap && it.name != null) {
                        this.messageCard = MessageCard().apply { name = it.name }
                    }
                }
            }
            put(id, fusedContact)
        }
    }

    init {
        preferences.registerOnPreferenceChangedListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "mode" -> {
                clearAll()
            }
        }
    }
}