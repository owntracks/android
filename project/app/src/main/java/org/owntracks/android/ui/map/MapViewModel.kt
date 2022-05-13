package org.owntracks.android.ui.map

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Location
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toLatLng
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageLocation.Companion.REPORT_TYPE_USER
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.support.MonitoringMode
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
    private val preferences: Preferences,
    private val locationRepo: LocationRepo,
    private val waypointsRepo: WaypointsRepo,
    @ApplicationContext private val applicationContext: Context,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    // controls who the currently selected contact is
    private val mutableCurrentContact = MutableLiveData<FusedContact?>()
    val currentContact: LiveData<FusedContact?>
        get() = mutableCurrentContact

    // controls the state of the bottom sheet on the map
    private val mutableBottomSheetHidden = MutableLiveData<Boolean>()
    val bottomSheetHidden: LiveData<Boolean>
        get() = mutableBottomSheetHidden

    // Controls where the map should set the camera to
    private val mutableMapCenter = MutableLiveData<LatLng>()
    val mapCenter: LiveData<LatLng>
        get() = mutableMapCenter

    // Where the map was last moved to. This might have been from an explicit
    // user action, or from the observation of mutableMapCenter changing
    private var lastScrolledMapCenter: MapLocationAndZoomLevel? = null

    // Shows the current distance to the selected contact
    private val mutableContactDistance = MutableLiveData(0f)
    val contactDistance: LiveData<Float>
        get() = mutableContactDistance

    // Shows the bearing to the selected contact
    private val mutableContactBearing = MutableLiveData(0f)
    val contactBearing: LiveData<Float>
        get() = mutableContactBearing

    // Shows the relative bearing from this device orientation to the contact
    private val mutableRelativeContactBearing = MutableLiveData(0f)
    val relativeContactBearing: LiveData<Float>
        get() = mutableRelativeContactBearing


    // Controls whether the myLocation button on the map is enabled
    private val mutableMyLocationEnabled = MutableLiveData(false)
    val myLocationEnabled: LiveData<Boolean>
        get() = mutableMyLocationEnabled

    // Controls the current map layer style
    private val mutableMapLayerStyle = MutableLiveData<MapLayerStyle>(preferences.mapLayerStyle)
    val mapLayerStyle: LiveData<MapLayerStyle>
        get() = mutableMapLayerStyle

    val currentLocation = LocationLiveData(applicationContext, viewModelScope)
    val regions = waypointsRepo.allLive
    val allContacts = contactsRepo.all

    val scope: CoroutineScope
        get() = viewModelScope

    private val mutableCurrentMonitoringMode: MutableLiveData<MonitoringMode> by lazy {
        MutableLiveData(preferences.monitoring)
    }

    val currentMonitoringMode: LiveData<MonitoringMode>
        get() = mutableCurrentMonitoringMode

    private val currentConnectionMode: MutableLiveData<Int> by lazy {
        MutableLiveData(preferences.mode)
    }

    fun getCurrentConnectionMode(): LiveData<Int> = currentConnectionMode

    val locationIdlingResource = SimpleIdlingResource("locationIdlingResource", false)

    var viewMode: ViewMode = ViewMode.Device

    init {
        preferences.registerOnPreferenceChangedListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        preferences.unregisterOnPreferenceChangedListener(this)
    }

    fun onMapReady() {
        when (viewMode) {
            is ViewMode.Contact -> {
                mutableCurrentContact.value?.run { setViewModeContact(this, true) }
            }
            is ViewMode.Free -> {
                setViewModeFree()
            }
            is ViewMode.Device -> {
                setViewModeDevice()
            }
        }
    }

    fun refreshGeocodeForContact(contact: FusedContact) {
        viewModelScope.launch {
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
        viewMode = ViewMode.Contact(center)
        mutableCurrentContact.value = contact
        mutableBottomSheetHidden.value = false
        refreshGeocodeForContact(contact)
        updateActiveContactDistanceAndBearing(contact)
        if (center && contact.latLng != null) mutableMapCenter.postValue(contact.latLng)
    }

    private fun setViewModeFree() {
        Timber.v("setting view mode: VIEW_FREE")
        viewMode = ViewMode.Free
        clearActiveContact()
    }

    private fun setViewModeDevice() {
        Timber.v("setting view mode: VIEW_DEVICE")
        viewMode = ViewMode.Device
        clearActiveContact()
        currentLocation.value?.apply {
            mutableMapCenter.postValue(this.toLatLng())
        } ?: run {
            Timber.e("no location available")
        }
    }

    @MainThread
    fun setLiveContact(contactId: String?) {
        contactId?.let {
            viewMode = ViewMode.Contact(true)
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

    fun contactPeekPopupmenuVisibility(): Boolean =
        mutableCurrentContact.value?.messageLocation != null || preferences.mode != MessageProcessorEndpointHttp.MODE_ID

    fun contactHasLocation(): Boolean {
        return mutableCurrentContact.value?.messageLocation != null
    }

    private fun updateActiveContactDistanceAndBearing(contact: FusedContact) {
        currentLocation.value?.run {
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
        contact: FusedContact,
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
        viewModelScope.launch { currentLocation.requestLocationUpdates() }
    }

    fun setBlueDotCurrentLocation(latLng: LatLng) {
        locationIdlingResource.setIdleState(true)
        locationRepo.setMapLocation(latLng)
    }

    fun setMapLocation(mapLocationAndZoomLevel: MapLocationAndZoomLevel) {
        lastScrolledMapCenter = mapLocationAndZoomLevel
    }

    fun getMapLocation(): MapLocationAndZoomLevel =
        lastScrolledMapCenter ?: locationRepo.currentMapLocation?.let {
            MapLocationAndZoomLevel(it, STARTING_ZOOM)
        } ?: locationRepo.currentPublishedLocation.value?.let {
            MapLocationAndZoomLevel(it.toLatLng(), STARTING_ZOOM)
        } ?: MapLocationAndZoomLevel(
            LatLng(
                STARTING_LATITUDE,
                STARTING_LONGITUDE
            ), STARTING_ZOOM
        )

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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "monitoring" -> mutableCurrentMonitoringMode.postValue(preferences.monitoring)
            "mode" -> {
                currentConnectionMode.postValue(preferences.mode)
                clearActiveContact()
            }
        }
    }

    fun setMonitoringMode(mode: MonitoringMode) {
        preferences.monitoring = mode
    }


    fun setMapLayerStyle(mapLayerStyle: MapLayerStyle) {
        preferences.mapLayerStyle = mapLayerStyle
        mutableMapLayerStyle.postValue(mapLayerStyle)
    }

    companion object {
        // Paris
        private const val STARTING_LATITUDE = 48.856826
        private const val STARTING_LONGITUDE = 2.292713
        private const val STARTING_ZOOM = 15.0
    }

    sealed class ViewMode {
        object Free : ViewMode()
        object Device : ViewMode()
        data class Contact(val follow: Boolean) : ViewMode()
    }
}