package org.owntracks.android.ui.map

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Location
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.scopes.ActivityScoped
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
import org.owntracks.android.support.Events.*
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.asin

@ActivityScoped
class MapViewModel @Inject constructor(
    private val contactsRepo: ContactsRepo,
    private val locationProcessor: LocationProcessor,
    private val messageProcessor: MessageProcessor,
    private val geocoderProvider: GeocoderProvider,
    private val preferences: Preferences
) : BaseViewModel<MapMvvm.View>(), MapMvvm.ViewModel<MapMvvm.View> {
    private val mutableLiveContact = MutableLiveData<FusedContact?>()
    private val liveBottomSheetHidden = MutableLiveData<Boolean>()
    private val liveCamera = MutableLiveData<LatLng>()
    private val liveLocation = MutableLiveData<Location?>()
    private val mainScope = MainScope()

    private val mutableContactDistance = MutableLiveData(0f)
    private val mutableContactBearing = MutableLiveData(0f)
    private val mutableRelativeContactBearing = MutableLiveData(0f)

    override val contact: LiveData<FusedContact?>
        get() = mutableLiveContact
    override val bottomSheetHidden: LiveData<Boolean>
        get() = liveBottomSheetHidden
    override val mapCenter: LiveData<LatLng>
        get() = liveCamera
    override val currentLocation: LiveData<Location?>
        get() = liveLocation
    val contactDistance: LiveData<Float>
        get() = mutableContactDistance
    val contactBearing: LiveData<Float>
        get() = mutableContactBearing
    val relativeContactBearing: LiveData<Float>
        get() = mutableRelativeContactBearing


    val locationIdlingResource = SimpleIdlingResource("locationIdlingResource", false)

    override fun saveInstanceState(outState: Bundle) {}
    override fun restoreInstanceState(savedInstanceState: Bundle) {}

    override fun onMapReady() {
        refreshMarkers()
        when (viewMode) {
            VIEW_CONTACT -> {
                mutableLiveContact.value?.run { setViewModeContact(this, true) }
            }
            VIEW_FREE -> {
                setViewModeFree()
            }
            else -> {
                setViewModeDevice()
            }
        }
    }

    override fun refreshMarkers() {
        for (c in contactsRepo.all.value!!.values) {
            view!!.updateMarker(c)
        }
    }

    override val mapLocationUpdateCallback: LocationCallback = object : LocationCallback {
        override fun onLocationResult(locationResult: LocationResult) {
            liveLocation.value = locationResult.lastLocation
            locationIdlingResource.setIdleState(true)
            if (viewMode == VIEW_DEVICE && liveCamera.value != locationResult.lastLocation.toLatLng()) {
                liveCamera.postValue(locationResult.lastLocation.toLatLng())
            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            // NOOP
        }
    }

    override fun refreshGeocodeForActiveContact() {
        mutableLiveContact.value?.also {
            mainScope.launch {
                it.messageLocation?.run { geocoderProvider.resolve(this) }
            }
        }
    }

    override fun sendLocation() {
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

    private fun setViewModeContact(c: FusedContact, center: Boolean) {
        viewMode = VIEW_CONTACT
        mutableLiveContact.postValue(c)
        refreshGeocodeForActiveContact()
        updateActiveContactDistanceAndBearing(c)
        liveBottomSheetHidden.postValue(false)
        if (center && c.latLng != null) liveCamera.postValue(c.latLng)
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
        if (liveLocation.value != null) {
            liveCamera.postValue(liveLocation.value!!.toLatLng())
        } else {
            Timber.e("no location available")
        }
    }

    @MainThread
    override fun setLiveContact(contactId: String?) {
        contactId?.let {
            viewMode = VIEW_CONTACT
            contactsRepo.getById(it)?.run(mutableLiveContact::setValue)
        }
    }

    private fun clearActiveContact() {
        mutableLiveContact.postValue(null)
        liveBottomSheetHidden.postValue(true)
    }

    override fun onBottomSheetClick() {
        view!!.setBottomSheetExpanded() // TODO use an observable
    }

    override fun onMenuCenterDeviceClicked() {
        setViewModeDevice()
    }

    override fun onClearContactClicked() {
        mutableLiveContact.value?.also {
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
        if (c.contact == mutableLiveContact.value) {
            clearActiveContact()
            setViewModeFree()
        }
        view!!.removeMarker(c.contact)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(c: FusedContact) {
        view!!.updateMarker(c)
        if (c == mutableLiveContact.value) {
            mutableLiveContact.postValue(c)
            if (c.latLng != null) {
                liveCamera.postValue(c.latLng)
            }
        }
    }

    fun contactPeekPopupmenuVisibility(): Boolean =
        mutableLiveContact.value?.messageLocation != null || preferences.mode != MessageProcessorEndpointHttp.MODE_ID

    override fun contactHasLocation(): Boolean {

        return mutableLiveContact.value?.messageLocation != null
    }

    private fun updateActiveContactDistanceAndBearing(contact: FusedContact) {
        liveLocation.value?.run {
            updateActiveContactDistanceAndBearing(this, contact)
        }
    }

    fun updateActiveContactDistanceAndBearing(currentLocation: Location) {
        mutableLiveContact.value?.run {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("UNUSED_PARAMETER")
    fun onEvent(e: ModeChanged?) {
        view!!.clearMarkers()
        clearActiveContact()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(e: MonitoringChanged?) {
        view!!.updateMonitoringModeMenu()
    }

    override fun onMapClick() {
        setViewModeFree()
    }

    override fun onMarkerClick(id: String) {
        setViewModeContact(id, false)
    }

    override fun onBottomSheetLongClick() {
        mutableLiveContact.value?.run {
            setViewModeContact(id, true)
        }

    }

    override val orientationSensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(maybeEvent: SensorEvent?) {
            maybeEvent?.let { event ->
                contact.value?.messageLocation?.let { contactLatLng ->
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

