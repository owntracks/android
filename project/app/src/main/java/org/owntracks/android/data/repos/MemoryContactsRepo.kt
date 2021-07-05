package org.owntracks.android.data.repos

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import org.owntracks.android.support.Events.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryContactsRepo @Inject constructor(
    private val eventBus: EventBus,
    private val contactsBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache
) : ContactsRepo {

    private val contacts = mutableMapOf<String,FusedContact>()
    override val all = MutableLiveData(contacts)

    override fun getById(id: String): FusedContact? {
        return contacts[id]
    }

    @Synchronized
    private fun put(id: String, contact: FusedContact) {
        Timber.v("new contact allocated id:%s, tid:%s", id, contact.trackerId)
        contacts[id] = contact
    }

    @MainThread
    @Synchronized
    override fun clearAll() {
        contacts.clear()
        contactsBitmapAndNameMemoryCache.evictAll()
    }

    @Synchronized
    override fun remove(id: String) {
        Timber.v("removing contact: %s", id)
        contacts.remove(id)?.run { eventBus.post(FusedContactRemoved(this)) }
    }

    @Synchronized
    override fun update(id: String, messageCard: MessageCard) {
        var c = getById(id)
        if (c != null) {
            c.messageCard = messageCard
            contactsBitmapAndNameMemoryCache.put(
                c.id,
                ContactBitmapAndName.CardBitmap(messageCard.name, null)
            )
            eventBus.post(c)
        } else {
            c = FusedContact(id)
            c.messageCard = messageCard
            contactsBitmapAndNameMemoryCache.put(
                c.id,
                ContactBitmapAndName.CardBitmap(messageCard.name, null)
            )
            put(id, c)
            eventBus.post(FusedContactAdded(c))
        }
    }

    @Synchronized
    override fun update(id: String, messageLocation: MessageLocation) {
        var fusedContact = getById(id)
        if (fusedContact != null) {
            // If timestamp of last location message is <= the new location message, skip update. We either received an old or already known message.
            if (fusedContact.setMessageLocation(messageLocation)) {
                eventBus.post(fusedContact)
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
            eventBus.post(FusedContactAdded(fusedContact))
        }
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") e: ModeChanged?) {
        clearAll()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") e: EndpointChanged?) {
        clearAll()
    }

    init {
        eventBus.register(this)
    }
}