package org.owntracks.android.ui.map

import android.hardware.SensorEventListener
import android.location.Location
import androidx.lifecycle.LiveData
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel

interface MapMvvm {
    interface View : MvvmView {
        fun setBottomSheetExpanded()
        fun setBottomSheetCollapsed()
        fun setBottomSheetHidden()
        fun updateMarker(contact: FusedContact)
        fun removeMarker(contact: FusedContact)
        fun clearMarkers()
        fun updateMonitoringModeMenu()
    }

    interface ViewModel<V : MvvmView?> : MvvmViewModel<V> {
        val orientationSensorEventListener: SensorEventListener
        val mapLocationUpdateCallback: LocationCallback
        val currentLocation: LiveData<Location?>

        fun onBottomSheetLongClick()
        fun onBottomSheetClick()
        fun onMenuCenterDeviceClicked()
        fun onClearContactClicked()
        fun onMapClick()
        fun onMarkerClick(id: String)
        fun setLiveContact(contactId: String?)
        fun onMapReady()
        fun refreshMarkers()
        val contact: LiveData<FusedContact?>
        val bottomSheetHidden: LiveData<Boolean>
        val mapCenter: LiveData<LatLng>
        fun sendLocation()
        fun contactHasLocation(): Boolean
        fun refreshGeocodeForActiveContact()
    }
}