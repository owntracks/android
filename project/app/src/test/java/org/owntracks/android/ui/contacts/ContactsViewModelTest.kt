package org.owntracks.android.ui.contacts

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.model.FusedContact

class ContactsViewModelTest {
    private val testContacts = MutableLiveData(
            mutableMapOf(
                    Pair("testContact1", FusedContact("testContact1")),
                    Pair("testContact2", FusedContact("testContact2")),
                    Pair("testContact3", FusedContact("testContact3")),
                    Pair("testContact4", FusedContact("testContact4")),
                    Pair("testContact5", FusedContact("testContact5")),
            )
    )
    private val mockContactsRepo: ContactsRepo = mock { on { all } doReturn testContacts }

    @Test
    fun `Contacts ViewModel outputs full list of contacts to view`() {

        val contactsViewModel = ContactsViewModel(mockContactsRepo)
        assertEquals(5, contactsViewModel.contacts.value?.keys?.size)
    }
}