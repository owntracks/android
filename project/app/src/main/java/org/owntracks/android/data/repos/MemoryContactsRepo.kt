package org.owntracks.android.data.repos

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import timber.log.Timber

@Singleton
class MemoryContactsRepo @Inject constructor(
    private val contactsBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache,
    private val preferences: Preferences
) : ContactsRepo, Preferences.OnPreferenceChangeListener {

    private val contacts = mutableMapOf<String, FusedContact>()
    override val all = MutableLiveData(contacts)

    override fun getById(id: String): FusedContact? {
        return contacts[id]
    }

    @Synchronized
    private fun put(id: String, contact: FusedContact) {
        Timber.v("new contact allocated id:%s, tid:%s", id, contact.trackerId)
        contacts[id] = contact
        all.postValue(contacts)
    }

    @MainThread
    @Synchronized
    override fun clearAll() {
        contacts.clear()
        contactsBitmapAndNameMemoryCache.evictAll()
        all.postValue(contacts)
    }

    @Synchronized
    override fun remove(id: String) {
        Timber.v("removing contact: %s", id)
        contacts.remove(id)
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
        var fusedContact = getById(id)
        if (fusedContact != null) {
            // If timestamp of last location message is <= the new location message, skip update. We either received an old or already known message.
            if (fusedContact.setMessageLocation(messageLocation)) {
                all.postValue(contacts)
            }
        } else {
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

    override fun onPreferenceChanged(properties: List<String>) {
        if (properties.contains("mode")) {
            clearAll()
        }
    }
}
