package org.owntracks.android.data.repos

import androidx.lifecycle.MutableLiveData
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation

interface ContactsRepo {
    val all: MutableLiveData<MutableMap<String, FusedContact>>
    fun getById(id: String): FusedContact?
    fun clearAll()
    fun remove(id: String)
    fun update(id: String, messageLocation: MessageLocation)
    fun update(id: String, messageCard: MessageCard)
    val revision: Long
}