package org.owntracks.android.data.repos

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.greenrobot.eventbus.EventBus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import org.owntracks.android.support.Events.EndpointChanged
import org.owntracks.android.ui.NoopAppShortcuts

class MemoryContactsRepoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var messageLocation: MessageLocation
    private lateinit var eventBus: EventBus
    private lateinit var contactBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache
    private var contactsRepo: ContactsRepo? = null

    private lateinit var preferences: Preferences

    @Before
    fun setup() {
        eventBus = mock {}
        val mockDisplayMetrics = DisplayMetrics()
        mockDisplayMetrics.densityDpi = 160
        mockResources = mock {
            on { getString(any()) } doReturn ""
            on { displayMetrics } doReturn mockDisplayMetrics
        }
        mockContext = mock {
            on { resources } doReturn mockResources
            on { packageName } doReturn javaClass.canonicalName
        }
        preferences = Preferences(mockContext, InMemoryPreferencesStore(), NoopAppShortcuts())

        messageLocation = MessageLocation()
        messageLocation.accuracy = 10
        messageLocation.altitude = 20
        messageLocation.battery = 30
        messageLocation.conn = "TestConn"
        messageLocation.latitude = 50.1
        messageLocation.longitude = 60.2
        messageLocation.timestamp = 123456789

        contactBitmapAndNameMemoryCache = ContactBitmapAndNameMemoryCache()

        contactsRepo = MemoryContactsRepo(eventBus, contactBitmapAndNameMemoryCache, preferences)
    }

    @Test
    fun `given an empty repo, when updating a contact with a location, then the contact is created with the location`() {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        val c = contactsRepo!!.getById(CONTACT_ID)
        assertEquals(messageLocation, c!!.messageLocation)
        assertEquals(messageLocation.timestamp, c.tst)
        assertEquals(CONTACT_ID, c.id)
    }

    @Test
    fun `given an empty repo, when updating a contact with a messageCard, then the contact is created with the card`() {
        val messageCard = MessageCard().apply {
            name = "TestName"
            face =
                "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"
        }
        contactsRepo!!.update(CONTACT_ID, messageCard)
        val c = contactsRepo!!.getById(CONTACT_ID)
        assertEquals(messageCard, c!!.messageCard)
        assertEquals(CONTACT_ID, c.id)
        assertEquals(
            "TestName",
            (contactBitmapAndNameMemoryCache[CONTACT_ID] as ContactBitmapAndName.CardBitmap).name
        )
    }

    @Test
    fun `given an repo containing a contact, when updating that contact with a messageCard, then the contact updated`() {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        val messageCard = MessageCard().apply {
            name = "TestName"
            face =
                "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"
        }
        contactsRepo!!.update(CONTACT_ID, messageCard)
        val c = contactsRepo!!.getById(CONTACT_ID)
        assertEquals(messageCard, c!!.messageCard)
        assertEquals(CONTACT_ID, c.id)
        assertEquals(
            "TestName",
            (contactBitmapAndNameMemoryCache[CONTACT_ID] as ContactBitmapAndName.CardBitmap).name
        )
    }

    @Test
    fun `given a non-empty repo, when removing a contact, then that contact is no longer in the repo`() {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        contactsRepo!!.remove(CONTACT_ID)
        assertNull(contactsRepo!!.getById(CONTACT_ID))
    }

    @Test
    fun `given a non-empty repo, when the mode change event is called, the repo is emptied`() {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        preferences.mode = ConnectionMode.HTTP
        assertTrue(contactsRepo!!.all.value!!.isEmpty())
    }

    @Test
    fun `given a non-empty repo, when the endpoint change event is called, the repo is emptied`() {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        (contactsRepo as MemoryContactsRepo?)!!.onEventMainThread(EndpointChanged())
        assertTrue(contactsRepo!!.all.value!!.isEmpty())
    }

    companion object {
        private const val CONTACT_ID = "abcd1234"
    }
}
