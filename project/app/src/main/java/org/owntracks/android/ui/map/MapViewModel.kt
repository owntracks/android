package org.owntracks.android.ui.map

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Location
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.location.*
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageLocation.Companion.REPORT_TYPE_USER
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.support.Events.FusedContactAdded
import org.owntracks.android.support.Events.FusedContactRemoved
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.SimpleIdlingResource
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.asin

@HiltViewModel
class MapViewModel @Inject constructor(
    private val contactsRepo: ContactsRepo,
    private val locationProcessor: LocationProcessor,
    private val messageProcessor: MessageProcessor,
    private val geocoderProvider: GeocoderProvider,
    private val preferences: Preferences
) : ViewModel() {
    private val mutableCurrentContact = MutableLiveData<FusedContact?>()
    private val mutableBottomSheetHidden = MutableLiveData<Boolean>()
    private val mutableMapCenter = MutableLiveData<LatLng>()
    private val mutableCurrentLocation = MutableLiveData<Location?>()
    private val mutableContactDistance = MutableLiveData(0f)
    private val mutableContactBearing = MutableLiveData(0f)
    private val mutableRelativeContactBearing = MutableLiveData(0f)
    private val mutableMyLocationEnabled = MutableLiveData(false)

    private val mainScope = MainScope()

    val currentContact: LiveData<FusedContact?>
        get() = mutableCurrentContact
    val bottomSheetHidden: LiveData<Boolean>
        get() = mutableBottomSheetHidden
    val mapCenter: LiveData<LatLng>
        get() = mutableMapCenter
    val currentLocation: LiveData<Location?>
        get() = mutableCurrentLocation
    val contactDistance: LiveData<Float>
        get() = mutableContactDistance
    val contactBearing: LiveData<Float>
        get() = mutableContactBearing
    val relativeContactBearing: LiveData<Float>
        get() = mutableRelativeContactBearing
    val myLocationEnabled: LiveData<Boolean>
        get() = mutableMyLocationEnabled

    val allContacts = contactsRepo.all


    val locationIdlingResource = SimpleIdlingResource("locationIdlingResource", false)

    fun onMapReady() {
        when (viewMode) {
            VIEW_CONTACT -> {
                mutableCurrentContact.value?.run { setViewModeContact(this, true) }
            }
            VIEW_FREE -> {
                setViewModeFree()
            }
            else -> {
                setViewModeDevice()
            }
        }
    }

    val mapLocationUpdateCallback: LocationCallback = object : LocationCallback {
        override fun onLocationResult(locationResult: LocationResult) {
            mutableCurrentLocation.value = locationResult.lastLocation
            locationIdlingResource.setIdleState(true)
            if (viewMode == VIEW_DEVICE && mutableMapCenter.value != locationResult.lastLocation.toLatLng()) {
                mutableMapCenter.postValue(locationResult.lastLocation.toLatLng())
            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            // NOOP
        }
    }

    fun refreshGeocodeForContact(contact: FusedContact) {
        mainScope.launch {
            contact.messageLocation?.run { geocoderProvider.resolve(this) }
        }
    }

    fun sendLocation() {
        currentLocation.value?.run {
            locationProcessor.onLocationChanged(this, REPORT_TYPE_USER)
        }
    }

    private fun setViewModeContact(contactId: String, center: Boolean) {
        val c = contactsRepo.getById(contactId)
        if (c != null) setViewModeContact(c, center) else Timber.e(
            "contact not found %s, ",
            contactId
        )
    }

    private fun setViewModeContact(contact: FusedContact, center: Boolean) {
        viewMode = VIEW_CONTACT
        mutableCurrentContact.value = contact
        mutableBottomSheetHidden.value = false
        refreshGeocodeForContact(contact)
        updateActiveContactDistanceAndBearing(contact)
        if (center && contact.latLng != null) mutableMapCenter.postValue(contact.latLng)
    }

    private fun setViewModeFree() {
        Timber.v("setting view mode: VIEW_FREE")
        viewMode = VIEW_FREE
        clearActiveContact()
    }

    private fun setViewModeDevice() {
        Timber.v("setting view mode: VIEW_DEVICE")
        viewMode = VIEW_DEVICE
        clearActiveContact()
        if (mutableCurrentLocation.value != null) {
            mutableMapCenter.postValue(mutableCurrentLocation.value!!.toLatLng())
        } else {
            Timber.e("no location available")
        }
    }

    @MainThread
    fun setLiveContact(contactId: String?) {
        contactId?.let {
            viewMode = VIEW_CONTACT
            contactsRepo.getById(it)?.run(mutableCurrentContact::setValue)
        }
    }

    private fun clearActiveContact() {
        mutableCurrentContact.postValue(null)
        mutableBottomSheetHidden.postValue(true)
    }

    fun onMenuCenterDeviceClicked() {
        setViewModeDevice()
    }

    fun onClearContactClicked() {
        mutableCurrentContact.value?.also {
            messageProcessor.queueMessageForSending(MessageClear().apply { topic = it.id })
            contactsRepo.remove(it.id)
        }
        clearActiveContact()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(e: FusedContactAdded) {
        onEvent(e.contact)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(c: FusedContactRemoved) {
        if (c.contact == mutableCurrentContact.value) {
            clearActiveContact()
            setViewModeFree()
        }
//        view!!.removeMarker(c.contact)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(c: FusedContact) {
//        view!!.updateMarker(c)
        if (c == mutableCurrentContact.value) {
            mutableCurrentContact.postValue(c)
            if (c.latLng != null) {
                mutableMapCenter.postValue(c.latLng)
            }
        }
    }

    fun contactPeekPopupmenuVisibility(): Boolean =
        mutableCurrentContact.value?.messageLocation != null || preferences.mode != MessageProcessorEndpointHttp.MODE_ID

    fun contactHasLocation(): Boolean {
        return mutableCurrentContact.value?.messageLocation != null
    }

    private fun updateActiveContactDistanceAndBearing(contact: FusedContact) {
        mutableCurrentLocation.value?.run {
            updateActiveContactDistanceAndBearing(this, contact)
        }
    }

    fun updateActiveContactDistanceAndBearing(currentLocation: Location) {
        mutableCurrentContact.value?.run {
            updateActiveContactDistanceAndBearing(currentLocation, this)
        }
    }

    private fun updateActiveContactDistanceAndBearing(
        currentLocation: Location,
        contact: FusedContact
    ) {
        contact.messageLocation?.run {
            val distanceBetween = FloatArray(2)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                latitude,
                longitude,
                distanceBetween
            )
            mutableContactDistance.postValue(distanceBetween[0])
            mutableContactBearing.postValue(distanceBetween[1])
            mutableRelativeContactBearing.postValue(distanceBetween[1])
        }
    }

    fun onMapClick() {
        setViewModeFree()
    }

    fun onMarkerClick(id: String) {
        setViewModeContact(id, false)
    }

    fun onBottomSheetLongClick() {
        mutableCurrentContact.value?.run {
            setViewModeContact(id, true)
        }

    }

    fun myLocationIsNowEnabled() {
        mutableMyLocationEnabled.postValue(true)
    }

    val orientationSensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(maybeEvent: SensorEvent?) {
            maybeEvent?.let { event ->
                currentContact.value?.messageLocation?.let { contactLatLng ->
                    currentLocation.value?.let { currentLocation ->
                        //Orientation is angle around the Z axis
                        val azimuth = (180 / Math.PI) * 2 * asin(event.values[2])
                        val distanceBetween = FloatArray(2)
                        Location.distanceBetween(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            contactLatLng.latitude,
                            contactLatLng.longitude,
                            distanceBetween
                        )
                        mutableRelativeContactBearing.postValue(distanceBetween[1] + azimuth.toFloat())
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //noop
        }
    }

    companion object {
        private const val VIEW_FREE = 0
        private const val VIEW_CONTACT = 1
        private const val VIEW_DEVICE = 2
        private var viewMode = VIEW_DEVICE
    }
}

