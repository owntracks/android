package org.owntracks.android.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import org.owntracks.android.BR
import org.owntracks.android.location.LatLng
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import timber.log.Timber

class FusedContact(id: String?) : BaseObservable() {
    @get:Bindable
    val id: String = if (!id.isNullOrEmpty()) id else "NOID"

    @get:Bindable
    var messageLocation: MessageLocation? = null
    internal var messageCard: MessageCard? = null

    @get:Bindable
    var tst: Long = 0
        private set

    fun setMessageLocation(messageLocation: MessageLocation): Boolean {
        if (tst > messageLocation.timestamp) return false
        Timber.v("update contact:%s, tst:%s", id, messageLocation.timestamp)
        messageLocation.setContact(this) // Allows to update fusedLocation if geocoder of messageLocation changed
        this.messageLocation = messageLocation
        tst = messageLocation.timestamp
        notifyMessageLocationPropertyChanged()
        return true
    }

    fun notifyMessageLocationPropertyChanged() {
        if (messageLocation != null) {
            Timber.d("Geocode location updated for %s: %s", id, messageLocation!!.geocode)
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
        get() = messageLocation?.geocode

    @get:Bindable
    val fusedName: String
        get() = (messageCard?.name ?: trackerId)

    @get:Bindable
    val fusedLocationAccuracy: Int
        get() = messageLocation?.accuracy ?: 0

    @get:Bindable
    val trackerId: String
        get() = messageLocation?.trackerId ?: id.replace("/", "")
            .let {
                return if (it.length > 2) {
                    it.substring(it.length - 2)
                } else {
                    it
                }
            }

    val latLng: LatLng?
        get() = messageLocation?.run { LatLng(latitude, longitude) }

    @get:Bindable
    val latLngString: String?
        get() = this.latLng?.toString()

    override fun toString(): String {
        return "FusedContact $id ($fusedName)"
    }
}
