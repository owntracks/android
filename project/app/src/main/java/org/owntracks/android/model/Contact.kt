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
  @get:Bindable val id: String = if (!id.isNullOrEmpty()) id else "NOID"

  @get:Bindable
  val displayName: String
    get() = name?.ifEmpty { trackerId } ?: trackerId

  // Set from a MessageCard
  private var name: String? = null
    private set(value) {
      field = value
      notifyPropertyChanged(BR.displayName)
    }

  @get:Bindable
  var latLng: LatLng? = null
    private set(value) {
      field = value
      notifyPropertyChanged(BR.latLng)
    }

  @get:Bindable
  var locationTimestamp: Long = 0
    private set(value) {
      field = value
      notifyPropertyChanged(BR.locationTimestamp)
    }

  @get:Bindable
  var face: String? = null
    private set(value) {
      field = value
      notifyPropertyChanged(BR.face)
    }

  fun setMessageCard(messageCard: MessageCard) {
    name = messageCard.name
    face = messageCard.face
  }

  fun setMessageLocation(messageLocation: MessageLocation): Boolean {
    if (locationTimestamp > messageLocation.timestamp) return false
    Timber.v("update contact:$id, tst:${messageLocation.timestamp}", id, messageLocation.timestamp)
    locationTimestamp = messageLocation.timestamp
    if (latLng != messageLocation.toLatLng()) {
      Timber.v("Contact ${this.id} has moved to $latLng")
      latLng = messageLocation.toLatLng()
    }
    trackerId = messageLocation.trackerId?.take(2) ?: messageLocation.topic.takeLast(2)
    locationAccuracy = messageLocation.accuracy
    altitude = messageLocation.altitude
    velocity = messageLocation.velocity
    battery = messageLocation.battery
    return true
  }

  @get:Bindable
  var locationAccuracy: Int = 0
    private set(value) {
      field = value
      notifyPropertyChanged(BR.locationAccuracy)
    }

  @get:Bindable
  var altitude: Int = 0
    private set(value) {
      field = value
      notifyPropertyChanged(BR.altitude)
    }

  @get:Bindable
  var velocity: Int = 0
    private set(value) {
      field = value
      notifyPropertyChanged(BR.velocity)
    }

  @get:Bindable
  var battery: Int? = null
    private set(value) {
      field = value
      notifyPropertyChanged(BR.battery)
    }

  @get:Bindable
  var geocodedLocation: String? = null
    private set(value) {
      field = value
      notifyPropertyChanged(BR.geocodedLocation)
    }

  @get:Bindable
  var trackerId: String = ""
    private set(value) {
      field = value
      notifyPropertyChanged(BR.trackerId)
      notifyPropertyChanged(BR.displayName)
    }

  fun geocodeLocation(geocoderProvider: GeocoderProvider, scope: CoroutineScope) {
    latLng?.let { scope.launch { geocodedLocation = geocoderProvider.resolve(it) } }
  }

  override fun toString(): String {
    return "Contact $id ($name)"
  }
}
