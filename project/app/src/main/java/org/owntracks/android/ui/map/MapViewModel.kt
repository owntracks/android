package org.owntracks.android.ui.map

import android.location.Location
import android.os.Bundle
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocationAvailability
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationResult
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.support.Events.*
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import timber.log.Timber
import javax.inject.Inject

@PerActivity
class MapViewModel @Inject constructor(contactsRepo: ContactsRepo, locationRepo: LocationProcessor, messageProcessor: MessageProcessor) : BaseViewModel<MapMvvm.View>(), MapMvvm.ViewModel<MapMvvm.View> {
    private val contactsRepo: ContactsRepo
    private val locationProcessor: LocationProcessor

    @get:Bindable
    override var activeContact: FusedContact? = null
        private set
    private var onLocationChangedListener: OnLocationChangedListener? = null
    private val messageProcessor: MessageProcessor
    private var location: Location? = null
    private val liveContact = MutableLiveData<FusedContact?>()
    private val liveBottomSheetHidden = MutableLiveData<Boolean>()
    private val liveCamera = MutableLiveData<LatLng>()
    val locationIdlingResource = SimpleIdlingResource("locationIdlingResource", false)
    override fun saveInstanceState(outState: Bundle) {}
    override fun restoreInstanceState(savedInstanceState: Bundle) {}

    override fun onMapReady() {
        for (c in contactsRepo.all.value!!.values) {
            view!!.updateMarker(c)
        }
        if (mode == VIEW_CONTACT && activeContact != null) setViewModeContact(activeContact!!, true) else if (mode == VIEW_FREE) {
            setViewModeFree()
        } else {
            setViewModeDevice()
        }
    }

    override val contact: LiveData<FusedContact?>
        get() = liveContact
    override val bottomSheetHidden: LiveData<Boolean>
        get() = liveBottomSheetHidden
    override val mapCenter: LiveData<LatLng>
        get() = liveCamera

    override val mapLocationUpdateCallback: LocationCallback = object : LocationCallback {
        override fun onLocationResult(locationResult: LocationResult) {
            Timber.d("Map location result received: $locationResult")
            location = locationResult.lastLocation
            view!!.enableLocationMenus() // TODO use an observable
            locationIdlingResource.setIdleState(true)
            if (mode == VIEW_DEVICE) {
                liveCamera.postValue(currentLocation)
            }
            if (onLocationChangedListener != null) {
                onLocationChangedListener!!.onLocationChanged(location)
            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            Timber.d("Map location availability: ${locationAvailability.locationAvailable}")
        }
    }

    override fun sendLocation() {
        locationProcessor.publishLocationMessage(MessageLocation.REPORT_TYPE_USER)
    }

    private fun setViewModeContact(contactId: String, center: Boolean) {
        val c = contactsRepo.getById(contactId)
        if (c != null) setViewModeContact(c, center) else Timber.e("contact not found %s, ", contactId)
    }

    private fun setViewModeContact(c: FusedContact, center: Boolean) {
        mode = VIEW_CONTACT
        Timber.v("contactId:%s, obj:%s ", c.id, activeContact)
        activeContact = c
        liveContact.postValue(c)
        liveBottomSheetHidden.postValue(false)
        if (center) liveCamera.postValue(c.latLng)
    }

    private fun setViewModeFree() {
        Timber.v("setting view mode: VIEW_FREE")
        mode = VIEW_FREE
        clearActiveContact()
    }

    private fun setViewModeDevice() {
        Timber.v("setting view mode: VIEW_DEVICE")
        mode = VIEW_DEVICE
        clearActiveContact()
        if (hasLocation()) {
            liveCamera.postValue(currentLocation)
        } else {
            Timber.e("no location available")
        }
    }

    override val currentLocation: LatLng?
        get() = if (location != null) LatLng(location!!.latitude, location!!.longitude) else null

    override fun restore(contactId: String?) {
        contactId?.let {
            Timber.v("restoring contact id:%s", it)
            setViewModeContact(it, true)
        }
    }

    override fun hasLocation(): Boolean {
        return location != null
    }

    private fun clearActiveContact() {
        activeContact = null
        liveContact.postValue(null)
        liveBottomSheetHidden.postValue(true)
    }

    override fun onBottomSheetClick() {
        view!!.setBottomSheetExpanded() // TODO use an observable
    }

    override fun onMenuCenterDeviceClicked() {
        setViewModeDevice()
    }

    override fun onClearContactClicked() {
        val m = MessageClear()
        if (activeContact != null) {
            m.topic = activeContact!!.id
            messageProcessor.queueMessageForSending(m)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(e: FusedContactAdded) {
        onEvent(e.contact)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(c: FusedContactRemoved) {
        if (c.contact == activeContact) {
            clearActiveContact()
            setViewModeFree()
        }
        view!!.removeMarker(c.contact)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(c: FusedContact) {
        view!!.updateMarker(c)
        if (c == activeContact) {
            liveContact.postValue(c)
            liveCamera.postValue(c.latLng)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(e: ModeChanged?) {
        view!!.clearMarkers()
        clearActiveContact()
    }

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
        setViewModeContact(activeContact!!.id, true)
    }

    companion object {
        private const val VIEW_FREE = 0
        private const val VIEW_CONTACT = 1
        private const val VIEW_DEVICE = 2
        private var mode = VIEW_DEVICE
    }

    init {
        Timber.v("onCreate")
        this.contactsRepo = contactsRepo
        this.messageProcessor = messageProcessor
        locationProcessor = locationRepo
    }
}