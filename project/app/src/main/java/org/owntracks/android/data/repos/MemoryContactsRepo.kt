package org.owntracks.android.data.repos

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.support.ContactImageProvider
import org.owntracks.android.support.Events.*
import timber.log.Timber
import javax.inject.Inject

class MemoryContactsRepo @Inject constructor(private val eventBus: EventBus, private val contactImageProvider: ContactImageProvider) : ContactsRepo {
    override val all = MutableLiveData<MutableMap<String, FusedContact>>(mutableMapOf())
    private var majorRevision: Long = 0
    override var revision: Long = 0
        get() = majorRevision + field

    override fun getById(id: String): FusedContact? {
        return all.value!![id]
    }

    @Synchronized
    private fun put(id: String, contact: FusedContact) {
        Timber.v("new contact allocated id:%s, tid:%s", id, contact.trackerId)
        val map = all.value!!
        map[id] = contact
        all.postValue(map)
    }

    @MainThread
    @Synchronized
    override fun clearAll() {
        all.value!!.clear()
        majorRevision -= MAJOR_STEP
        revision = 0
        contactImageProvider.invalidateCache()
    }

    @Synchronized
    override fun remove(id: String) {
        Timber.v("removing contact: %s", id)
        val c = all.value!!.remove(id)
        if (c != null) {
            c.setDeleted()
            eventBus.post(FusedContactRemoved(c))
            majorRevision -= MAJOR_STEP
            revision = 0
        }
    }

    @Synchronized
    override fun update(id: String, messageCard: MessageCard) {
        var c = getById(id)
        if (c != null) {
            c.messageCard = messageCard
            contactImageProvider.invalidateCacheLevelCard(c.id)
            revision++
            eventBus.post(c)
        } else {
            c = FusedContact(id)
            c.messageCard = messageCard
            contactImageProvider.invalidateCacheLevelCard(c.id)
            put(id, c)
            revision++
            eventBus.post(FusedContactAdded(c))
        }
    }

    @Synchronized
    override fun update(id: String, messageLocation: MessageLocation) {
        var fusedContact = getById(id)
        if (fusedContact != null) {
            // If timestamp of last location message is <= the new location message, skip update. We either received an old or already known message.
            if (fusedContact.setMessageLocation(messageLocation)) {
                revision++
                eventBus.post(fusedContact)
            }
        } else {
            fusedContact = FusedContact(id)
            fusedContact.setMessageLocation(messageLocation)
            put(id, fusedContact)
            revision++
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

    companion object {
        const val MAJOR_STEP: Long = 1000000
    }

    init {
        eventBus.register(this)
    }
}