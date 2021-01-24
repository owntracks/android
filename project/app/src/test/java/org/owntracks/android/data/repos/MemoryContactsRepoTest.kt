package org.owntracks.android.data.repos

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.greenrobot.eventbus.EventBus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.support.ContactImageProvider
import org.owntracks.android.support.Events.EndpointChanged
import org.owntracks.android.support.Events.ModeChanged

class MemoryContactsRepoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var messageLocation: MessageLocation
    private lateinit var eventBus: EventBus
    private var contactsRepo: ContactsRepo? = null

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
        val contactImageProvider = ContactImageProvider(mockContext)

        messageLocation = MessageLocation()
        messageLocation.accuracy = 10
        messageLocation.altitude = 20
        messageLocation.battery = 30
        messageLocation.conn = "TestConn"
        messageLocation.latitude = 50.1
        messageLocation.longitude = 60.2
        messageLocation.timestamp = 123456789
        contactsRepo = MemoryContactsRepo(eventBus, contactImageProvider)
    }

    @Test
    fun repoCorrectlyUpdatesContactWithMessageLocation() {
        assertEquals(0, contactsRepo!!.revision)
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        assertEquals(1, contactsRepo!!.revision)
        val c = contactsRepo!!.getById(CONTACT_ID)
        assertEquals(messageLocation, c!!.messageLocation.value)
        assertEquals(messageLocation.timestamp, c.tst)
        assertEquals(CONTACT_ID, c.id)
    }

    @Test
    fun repoCorrectlyRemovesContactById() {
        assertEquals(0, contactsRepo!!.revision)
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        assertEquals(1, contactsRepo!!.revision)
        val c = contactsRepo!!.getById(CONTACT_ID)
        assertFalse(c!!.isDeleted)
        contactsRepo!!.remove(CONTACT_ID)
        assertEquals(-MemoryContactsRepo.MAJOR_STEP, contactsRepo!!.revision)
        assertTrue(c.isDeleted)
        assertNull(contactsRepo!!.getById(CONTACT_ID))
    }

    @Test
    fun repoCorrectlyHandlesEventModeChanged() {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        (contactsRepo as MemoryContactsRepo?)!!.onEventMainThread(ModeChanged(0, 1))
        assertTrue(contactsRepo!!.all.value!!.isEmpty())
    }

    @Test
    fun repoCorrectlyHandlesEventEndpointChanged() {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        (contactsRepo as MemoryContactsRepo?)!!.onEventMainThread(EndpointChanged())
        assertTrue(contactsRepo!!.all.value!!.isEmpty())
    }

    companion object {
        private const val CONTACT_ID = "abcd1234"
    }
}