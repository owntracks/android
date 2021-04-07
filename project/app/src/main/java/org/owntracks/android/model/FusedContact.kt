package org.owntracks.android.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import org.owntracks.android.BR
import org.owntracks.android.location.LatLng
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import timber.log.Timber

class FusedContact(id: String?) : BaseObservable() {
    @get:Bindable
    val id: String = if (id != null && id.isNotEmpty()) id else "NOID"

    @get:Bindable
    val messageLocation = MutableLiveData<MessageLocation?>()
    internal var messageCard: MessageCard? = null

    @get:Bindable
    @set:Bindable
    var imageProvider = 0

    @get:Bindable
    var tst: Long = 0
        private set

    fun setMessageLocation(messageLocation: MessageLocation): Boolean {
        if (tst > messageLocation.timestamp) return false
        Timber.v("update contact:%s, tst:%s", id, messageLocation.timestamp)
        messageLocation.setContact(this) // Allows to update fusedLocation if geocoder of messageLocation changed
        this.messageLocation.postValue(messageLocation)
        tst = messageLocation.timestamp
        notifyMessageLocationPropertyChanged()
        return true
    }

    fun notifyMessageLocationPropertyChanged() {
        if (messageLocation.value != null) {
            Timber.d("Geocode location updated for %s: %s", id, messageLocation.value!!.geocode)
        }
        notifyPropertyChanged(BR.fusedName)
        notifyPropertyChanged(BR.messageLocation)
        notifyPropertyChanged(BR.geocodedLocation)
        notifyPropertyChanged(BR.fusedLocationAccuracy)
        notifyPropertyChanged(BR.tst)
        notifyPropertyChanged(BR.trackerId)
        notifyPropertyChanged(BR.id)
    }

    @get:Bindable
    val geocodedLocation: String?
        get() = messageLocation.value?.geocode


    @Bindable
    fun getMessageCard(): MessageCard? {
        return messageCard
    }

    @get:Bindable
    val fusedName: String?
        get() = if (hasCard() && getMessageCard()!!.hasName()) getMessageCard()!!.name else trackerId

    @get:Bindable
    val fusedLocationAccuracy: String
        get() = if (hasLocation()) messageLocation.value!!.accuracy.toString() else 0.toString()

    fun hasLocation(): Boolean {
        return messageLocation.value != null
    }

    fun hasCard(): Boolean {
        return messageCard != null
    }

    @get:Bindable
    val trackerId: String
        get() = if (hasLocation() && messageLocation.value!!.hasTrackerId()) messageLocation.value!!.trackerId!! else {
            val id = id.replace("/", "")
            if (id.length > 2) {
                id.substring(id.length - 2)
            } else id
        }
    val latLng: LatLng
        get() = LatLng(messageLocation.value!!.latitude, messageLocation.value!!.longitude)
    var isDeleted = false
        private set

    fun setDeleted() {
        isDeleted = true
    }
}