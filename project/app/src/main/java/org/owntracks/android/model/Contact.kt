package org.owntracks.android.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toLatLng
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import timber.log.Timber

class Contact(id: String) {
  val id: String = id.ifEmpty { "NOID" }

  val displayName: String
    get() = name?.ifEmpty { trackerId } ?: trackerId

  // Set from a MessageCard
  private var name: String? = null
    private set(value) {
      field = value
    }

  var locationAccuracy: Int = 0
  var altitude: Int = 0
  var velocity: Int = 0
  var battery: Int? = null
  var geocodedLocation: String? = null
  var trackerId: String = id.takeLast(2)

  var latLng: LatLng? = null
    private set

  var locationTimestamp: Long = 0
    private set

  var face: String? = null
    private set

  fun setMessageCard(messageCard: MessageCard) {
    name = messageCard.name
    face = messageCard.face
  }

  fun setLocationFromMessageLocation(messageLocation: MessageLocation): Boolean {
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

  fun setLocationFromMessageTransition(messageLocation: MessageTransition): Boolean {
    if (locationAccuracy > messageLocation.timestamp) return false
    locationTimestamp = messageLocation.timestamp
    if (latLng != messageLocation.toLatLng()) {
      Timber.v("Contact ${this.id} has moved to $latLng")
      latLng = messageLocation.toLatLng()
    }
    locationAccuracy = messageLocation.accuracy
    return true
  }

  fun geocodeLocation(geocoderProvider: GeocoderProvider, scope: CoroutineScope) {
    latLng?.let { scope.launch { geocodedLocation = geocoderProvider.resolve(it) } }
  }

  override fun toString(): String {
    return "Contact $id ($name)"
  }
}
