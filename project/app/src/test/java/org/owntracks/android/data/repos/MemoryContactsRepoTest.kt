package org.owntracks.android.data.repos

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.owntracks.android.location.toLatLng
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactBitmapAndName
import org.owntracks.android.support.ContactBitmapAndNameMemoryCache
import org.owntracks.android.test.SimpleIdlingResource

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryContactsRepoTest {

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var mockResources: Resources
  private lateinit var mockContext: Context
  private lateinit var messageLocation: MessageLocation
  private lateinit var messageCard: MessageCard
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

    messageLocation =
        MessageLocation().apply {
          accuracy = 10
          altitude = 20
          battery = 30
          conn = "TestConn"
          latitude = 50.1
          longitude = 60.2
          timestamp = 123456789
          trackerId = "aa"
          topic = "owntracks/test/name"
        }

    messageCard = MessageCard().apply { name = "TESTNAME" }

    contactBitmapAndNameMemoryCache = ContactBitmapAndNameMemoryCache()

    contactsRepo = MemoryContactsRepo(contactBitmapAndNameMemoryCache)
  }

  @Test
  fun `given an empty repo, when updating a contact with a location, then the contact is created with the location`() =
      runTest {
        contactsRepo!!.run {
          update(CONTACT_ID, messageLocation)
          getById(CONTACT_ID)!!.let { c ->
            assertEquals(messageLocation.battery, c.battery)
            assertEquals(messageLocation.toLatLng(), c.latLng)
            assertEquals(messageLocation.altitude, c.altitude)
            assertEquals(messageLocation.timestamp, c.locationTimestamp)
            assertEquals("aa", c.trackerId)
            assertEquals(CONTACT_ID, c.id)
          }
        }
      }

  @Test
  fun `given an empty repo, when updating a contact with a location without a tid, then the contact is created with the fallback displayName`() =
      runTest {
        contactsRepo!!.run {
          update(CONTACT_ID, messageLocation.apply { trackerId = null })
          getById(CONTACT_ID)!!.let { c ->
            assertEquals(messageLocation.battery, c.battery)
            assertEquals(messageLocation.toLatLng(), c.latLng)
            assertEquals(messageLocation.altitude, c.altitude)
            assertEquals(messageLocation.timestamp, c.locationTimestamp)
            assertEquals("me", c.trackerId)
            assertEquals(CONTACT_ID, c.id)
          }
        }
      }

  @Test
  fun `given an empty repo, when updating a contact with a card, then the contact is created with the card`() =
      runTest {
        contactsRepo!!.run {
          update(CONTACT_ID, messageCard)
          getById(CONTACT_ID)!!.let { c ->
            assertEquals(messageCard.name, c.displayName)
            assertEquals(messageCard.face, c.face)
            assertEquals(CONTACT_ID, c.id)
          }
        }
      }

  @Test
  fun `given a non-empty repo, when updating the card for an existing contact, then a card updated event is emitted`() =
      runTest {
        contactsRepo!!.run {
          update(CONTACT_ID, messageLocation)
          val values = mutableListOf<ContactsRepoChange>()
          backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repoChangedEvent.toList(values)
          }
          update(CONTACT_ID, messageCard)
          assertEquals(1, values.count())
          assert(values[0] is ContactsRepoChange.ContactCardUpdated)
          assertEquals(CONTACT_ID, (values[0] as ContactsRepoChange.ContactCardUpdated).contact.id)
        }
      }

  @Test
  fun `given a non-empty repo, when updating the location for an existing contact, then a location updated event is emitted`() =
      runTest {
        contactsRepo!!.run {
          update(CONTACT_ID, messageCard)
          val values = mutableListOf<ContactsRepoChange>()
          backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repoChangedEvent.toList(values)
          }
          update(CONTACT_ID, messageLocation)
          assertEquals(1, values.count())
          assert(values[0] is ContactsRepoChange.ContactLocationUpdated)
          assertEquals(
              CONTACT_ID, (values[0] as ContactsRepoChange.ContactLocationUpdated).contact.id)
        }
      }

  @Test
  fun `given a non-empty repo, when removing a contact, then that contact is no longer in the repo`() =
      runTest {
        contactsRepo!!.run {
          update(CONTACT_ID, messageLocation)
          remove(CONTACT_ID)
        }
        assertNull(contactsRepo!!.getById(CONTACT_ID))
      }

  @Test
  fun `given a non-empty repo, when removing a contact, then a ContactRemoved event is emitted`() =
      runTest {
        contactsRepo!!.run {
          update(CONTACT_ID, messageLocation)

          val values = mutableListOf<ContactsRepoChange>()
          backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repoChangedEvent.toList(values)
          }
          remove(CONTACT_ID)
          assertEquals(1, values.count())
          assert(values[0] is ContactsRepoChange.ContactRemoved)
          assertEquals(CONTACT_ID, (values[0] as ContactsRepoChange.ContactRemoved).contact.id)
        }
      }

  @Test
  fun `given a non-empty repo, when calling clearAll, then the repo is cleared`() = runTest {
    contactsRepo!!.run {
      update(CONTACT_ID, messageLocation)
      clearAll()
    }
    assertTrue(contactsRepo!!.all.isEmpty())
  }

  @Test
  fun `given a non-empty repo, when calling clearAll, then a Cleared event is emitted`() = runTest {
    contactsRepo!!.run {
      update(CONTACT_ID, messageLocation)
      val values = mutableListOf<ContactsRepoChange>()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        repoChangedEvent.toList(values)
      }
      clearAll()
      assertEquals(1, values.count())
      assert(values[0] is ContactsRepoChange.AllCleared)
    }
  }

  @Test
  fun `given a repo, when a contact is added, then a ContactAdded event is emitted`() = runTest {
    contactsRepo!!.run {
      val values = mutableListOf<ContactsRepoChange>()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        repoChangedEvent.toList(values)
      }
      update(CONTACT_ID, messageLocation)
      assertEquals(1, values.count())
      assert(values[0] is ContactsRepoChange.ContactAdded)
      assertEquals(CONTACT_ID, (values[0] as ContactsRepoChange.ContactAdded).contact.id)
    }
  }

  @Test
  fun `given a repo, when a contact is added with a location and the face cache contains a card, then the card is added to the contact`() =
      runTest {
        contactsRepo!!.run {
          contactBitmapAndNameMemoryCache.put(
              CONTACT_ID, ContactBitmapAndName.CardBitmap("TESTNAME", null))
          update(CONTACT_ID, messageLocation)
          assertEquals("TESTNAME", getById(CONTACT_ID)!!.displayName)
        }
      }

  companion object {
    private const val CONTACT_ID = "abcd1234"
  }
}
