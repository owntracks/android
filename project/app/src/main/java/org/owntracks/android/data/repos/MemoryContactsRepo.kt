package org.owntracks.android.data.repos

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.owntracks.android.model.Contact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import timber.log.Timber

@Singleton
class MemoryContactsRepo
@Inject
constructor(
    private val contactBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache,
) : ContactsRepo {

  private val contacts = mutableMapOf<String, Contact>()
  private val repoLock = Mutex()

  private val mutableRepoChangedEvent = MutableSharedFlow<ContactsRepoChange>()

  override val repoChangedEvent: SharedFlow<ContactsRepoChange> = mutableRepoChangedEvent
  override val all: Map<String, Contact>
    get() = contacts

  override fun getById(id: String): Contact? {
    return contacts[id]
  }

  override suspend fun clearAll() {
    Timber.i("Clearing all contacts. Waiting for lock")
    measureTime {
          repoLock.withLock {
            Timber.v("Lock acquired")
            contacts.clear()
            contactBitmapAndNameMemoryCache.evictAll()
            mutableRepoChangedEvent.emit(ContactsRepoChange.AllCleared)
          }
        }
        .also { Timber.d("Cleared all contacts in $it") }
  }

  override suspend fun remove(id: String) {
    Timber.v("removing contact: $id. waiting for lock")
    measureTime {
          repoLock.withLock {
            Timber.v("Lock acquired")
            contacts.remove(id)?.run {
              mutableRepoChangedEvent.emit(ContactsRepoChange.ContactRemoved(this))
            }
          }
        }
        .also { Timber.d("remove contact $id took $it") }
  }

  override suspend fun update(id: String, messageCard: MessageCard) {
    measureTime {
          Timber.v("updating contact card for contact $id. waiting for repoLock")
          repoLock.withLock {
            Timber.v("Lock acquired")

            getById(id)?.apply {
              // We just received new contact details, so invalidate the cache entry.
              contactBitmapAndNameMemoryCache.remove(id)
              this.setMessageCard(messageCard)
              mutableRepoChangedEvent.emit(ContactsRepoChange.ContactCardUpdated(this))
            } ?: run { Contact(id).apply { setMessageCard(messageCard) }.also { put(id, it) } }
          }
        }
        .also { Timber.d("update contact card for contact $id took $it") }
  }

  override suspend fun update(id: String, messageLocation: MessageLocation) {
    Timber.v("Updating location for contact $id from $messageLocation. waiting for repoLock")
    updateContactLocation(id) { contact: Contact ->
      contact.setLocationFromMessageLocation(messageLocation)
    }
  }

  override suspend fun update(id: String, messageTransition: MessageTransition) {
    Timber.v("Updating location for contact $id from $messageTransition. waiting for repoLock")
    updateContactLocation(id) { contact: Contact ->
      contact.setLocationFromMessageTransition(messageTransition)
    }
  }

  private suspend fun updateContactLocation(id: String, updateLocation: (Contact) -> Boolean) {

    measureTime {
          repoLock.withLock {
            getById(id)?.apply {
              // If timestamp of last location message is <= the new location message, skip update.
              // We either received an old or already known message.
              if (updateLocation(this)) {
                mutableRepoChangedEvent.emit(ContactsRepoChange.ContactLocationUpdated(this))
              }
            }
                ?: run { // If getById is null, we have not seen this contact id before
                  Contact(id)
                      .apply {
                        updateLocation(this)
                        // We may have seen this contact id before, and it may have been removed
                        // from the repo Check the cache to see if we have a name
                        contactBitmapAndNameMemoryCache[id]?.also {
                          if (it is ContactBitmapAndName.CardBitmap && it.name != null) {
                            setMessageCard(MessageCard().apply { name = it.name })
                          }
                        }
                      }
                      .also { put(id, it) }
                }
          }
        }
        .also { Timber.d("update location for contact $id took $it") }
  }

  private suspend fun put(id: String, contact: Contact) {
    Timber.v("new contact allocated id=$id, tid=${contact.trackerId}")
    contacts[id] = contact
    mutableRepoChangedEvent.emit(ContactsRepoChange.ContactAdded(contact))
  }
}
