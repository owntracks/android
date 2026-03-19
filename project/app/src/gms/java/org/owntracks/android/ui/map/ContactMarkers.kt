package org.owntracks.android.ui.map

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.model.Contact
import org.owntracks.android.support.ContactImageBindingAdapter

/**
 * Renders all contact markers on the Google Map. Each contact with a valid location is displayed as
 * a marker with their avatar image.
 *
 * @param contacts Map of contact IDs to Contact objects
 * @param contactImageBindingAdapter Adapter for loading contact avatar bitmaps
 * @param onMarkerClick Callback when a marker is clicked, receives the contact ID
 */
@Composable
fun ContactMarkers(
    contacts: Map<String, Contact>,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    onMarkerClick: (String) -> Unit
) {
  contacts.values.forEach { contact ->
    contact.latLng?.let { latLng ->
      ContactMarker(
          contact = contact,
          contactImageBindingAdapter = contactImageBindingAdapter,
          onMarkerClick = { onMarkerClick(contact.id) })
    }
  }
}

/**
 * Renders a single contact marker with their avatar image.
 *
 * @param contact The contact to display
 * @param contactImageBindingAdapter Adapter for loading the contact's avatar bitmap
 * @param onMarkerClick Callback when this marker is clicked
 */
@Composable
private fun ContactMarker(
    contact: Contact,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    onMarkerClick: () -> Unit
) {
  // Load the contact's avatar bitmap asynchronously
  val bitmap by
      produceState<Bitmap?>(initialValue = null, contact.id, contact.face, contact.trackerId) {
        value = contactImageBindingAdapter.getBitmapFromCache(contact)
      }

  // Only render the marker once we have both a location and a bitmap
  val latLng = contact.latLng
  if (bitmap != null && latLng != null) {
    Marker(
        state = MarkerState(position = latLng.toGMSLatLng()),
        icon = bitmap!!.toBitmapDescriptor(),
        anchor = Offset(0.5f, 0.5f),
        tag = contact.id,
        onClick = {
          onMarkerClick()
          true
        })
  }
}
