package org.owntracks.android.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.owntracks.android.BR
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toLatLng
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import timber.log.Timber

class Contact(id: String?) : BaseObservable() {
    @get:Bindable
    val id: String = if (!id.isNullOrEmpty()) id else "NOID"

    @get:Bindable
    var messageLocation: MessageLocation? = null
        private set(value) {
            val prevLocation = messageLocation?.toLatLng()
            field = value
            if (prevLocation != value?.toLatLng()) {
                Timber.v("contact ${this.id} has moved to ${value?.toLatLng()}")
                notifyPropertyChanged(BR.geocodedLocation)
            }
            notifyPropertyChanged(BR.messageLocation)
            notifyPropertyChanged(BR.name)
            notifyPropertyChanged(BR.fusedLocationAccuracy)
            notifyPropertyChanged(BR.tst)
            notifyPropertyChanged(BR.trackerId)
            notifyPropertyChanged(BR.id)
            notifyPropertyChanged(BR.latLng)
        }
    internal var messageCard: MessageCard? = null

    @get:Bindable
    var tst: Long = 0
        get() = messageLocation?.timestamp ?: 0

    fun setMessageLocation(messageLocation: MessageLocation): Boolean {
        if (tst > messageLocation.timestamp) return false
        Timber.v("update contact:$id, tst:${messageLocation.timestamp}", id, messageLocation.timestamp)
        this.messageLocation = messageLocation
        return true
    }

    @get:Bindable
    val geocodedLocation: String?
        get() = messageLocation?.geocode

    @get:Bindable
    val name: String
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

    @get:Bindable
    val latLng: LatLng?
        get() = messageLocation?.run { LatLng(latitude, longitude) }

    override fun toString(): String {
        return "Contact $id ($name)"
    }

    fun geocodeLocation(geocoderProvider: GeocoderProvider, scope: CoroutineScope) {
        messageLocation?.let {
            scope.launch {
                geocoderProvider.resolve(it)
                notifyPropertyChanged(BR.geocodedLocation)
            }
        }
    }
}
