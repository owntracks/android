package org.owntracks.android.data.repos

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import org.owntracks.android.support.SimpleIdlingResource

class MemoryContactsRepoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var messageLocation: MessageLocation
    private lateinit var contactBitmapAndNameMemoryCache: ContactBitmapAndNameMemoryCache
    private var contactsRepo: ContactsRepo? = null
    private val mockIdlingResource = SimpleIdlingResource("mock", true)
    private lateinit var preferences: Preferences

    @Before
    fun setup() {
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
        preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)

        messageLocation = MessageLocation().apply {
            accuracy = 10
            altitude = 20
            battery = 30
            conn = "TestConn"
            latitude = 50.1
            longitude = 60.2
            timestamp = 123456789
        }

        contactBitmapAndNameMemoryCache = ContactBitmapAndNameMemoryCache()

        contactsRepo = MemoryContactsRepo(contactBitmapAndNameMemoryCache, preferences)
    }

    @Test
    fun `given an empty repo, when updating a contact with a location, then the contact is created with the location`() = runTest {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        val c = contactsRepo!!.getById(CONTACT_ID)
        assertEquals(messageLocation, c!!.messageLocation)
        assertEquals(messageLocation.timestamp, c.tst)
        assertEquals(CONTACT_ID, c.id)
    }

    @Test
    fun `given a non-empty repo, when removing a contact, then that contact is no longer in the repo`() = runTest {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        contactsRepo!!.remove(CONTACT_ID)
        assertNull(contactsRepo!!.getById(CONTACT_ID))
    }

    @Test
    fun `given a non-empty repo, when the mode change event is called, the repo is emptied`() = runTest {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        preferences.mode = ConnectionMode.HTTP
        assertTrue(contactsRepo!!.all.isEmpty())
    }

    @Test
    fun `given a non-empty repo, when the endpoint change event is called, the repo is emptied`() = runTest {
        contactsRepo!!.update(CONTACT_ID, messageLocation)
        preferences.mode = ConnectionMode.HTTP
        assertTrue(contactsRepo!!.all.isEmpty())
    }

    companion object {
        private const val CONTACT_ID = "abcd1234"
    }
}
