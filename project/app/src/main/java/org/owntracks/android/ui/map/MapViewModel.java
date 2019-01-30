package org.owntracks.android.ui.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.databinding.Bindable;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.LocationProcessor;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.util.Collection;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity
public class MapViewModel extends BaseViewModel<MapMvvm.View> implements MapMvvm.ViewModel<MapMvvm.View>, LocationSource, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
    private final ContactsRepo contactsRepo;
    private final LocationProcessor locationProcessor;
    private FusedContact activeContact;
    private LocationSource.OnLocationChangedListener mListener;
    private MessageProcessor messageProcessor;
    Location mLocation;

    private static final int VIEW_FREE = 0;
    private static final int VIEW_CONTACT = 1;
    private static final int VIEW_DEVICE = 2;


    private static int mode = VIEW_DEVICE;
    private MutableLiveData<FusedContact> liveContact = new MutableLiveData<>();
    private MutableLiveData<Boolean> liveBottomSheetHidden = new MutableLiveData<>();
    private MutableLiveData<LatLng> liveCamera = new MutableLiveData<>();

    @Inject
    public MapViewModel(ContactsRepo contactsRepo, LocationProcessor locationRepo, MessageProcessor messageProcessor) {
        Timber.v("onCreate");
        this.contactsRepo = contactsRepo;
        this.messageProcessor = messageProcessor;
        this.locationProcessor = locationRepo;
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
    }

    @Override
    public void restoreInstanceState(@NonNull Bundle savedInstanceState) {
    }

    @Override
    public LocationSource getMapLocationSource() {
        return this;
    }

    @Override
    public GoogleMap.OnMapClickListener getOnMapClickListener() {
        return this;
    }

    @Override
    public GoogleMap.OnMarkerClickListener getOnMarkerClickListener() {
        return this;
    }

    @Override
    public void onMapReady() {
        for(Object c : contactsRepo.getAllAsList()) {
            getView().updateMarker(FusedContact.class.cast(c));
        }

        if(mode == VIEW_CONTACT && activeContact != null)
            setViewModeContact(activeContact, true);
        else if (mode == VIEW_FREE) {
            setViewModeFree();
        } else {
            setViewModeDevice();
        }
    }

    @Override
    public LiveData<FusedContact> getContact() {
        return liveContact;
    }

    @Override
    public LiveData<Boolean> getBottomSheetHidden() {
        return liveBottomSheetHidden;
    }

    @Override
    public LiveData<LatLng> getCenter() {
        return liveCamera;
    }

    @Override
    public void sendLocation() {
        locationProcessor.publishLocationMessage(MessageLocation.REPORT_TYPE_USER);
    }


    private void setViewModeContact(@NonNull String contactId, boolean center) {
        FusedContact c = contactsRepo.getById(contactId);
        if(c != null)
            setViewModeContact(c, center);
        else
            Timber.e("contact not found %s, ", contactId);
    }

    private void setViewModeContact(@NonNull FusedContact c, boolean center) {
        mode = VIEW_CONTACT;
        Timber.v("contactId:%s, obj:%s ", c.getId(), activeContact);

        activeContact = c;

        liveContact.postValue(c);
        liveBottomSheetHidden.postValue(false);

        if(center)
            liveCamera.postValue(c.getLatLng());

    }

    private void setViewModeFree() {
        Timber.v("setting view mode: VIEW_FREE");
        mode = VIEW_FREE;
        clearActiveContact();
    }

    private void setViewModeDevice() {
        Timber.v("setting view mode: VIEW_DEVICE");

        mode = VIEW_DEVICE;
        clearActiveContact();
        if(hasLocation()) {
            liveCamera.postValue(getCurrentLocation());
        } else {
            Timber.e("no location available");
        }
    }



    @Override
    @Nullable
    public LatLng getCurrentLocation() {
        return mLocation != null ? new LatLng(mLocation.getLatitude(), mLocation.getLongitude()) : null;
    }

    @Override
    @Bindable
    public FusedContact getActiveContact() {
        return activeContact;
    }

    @Override
    public Collection<FusedContact> getContacts() {
        return this.contactsRepo.getAllAsList();
    }

    @Override
    public void restore(@NonNull String contactId) {
        Timber.v("restoring contact id:%s", contactId);
        setViewModeContact(contactId, true);
    }

    @Override
    public boolean hasLocation() {
        return mLocation != null;
    }


    private void clearActiveContact() {
        activeContact = null;
        liveContact.postValue(null);
        liveBottomSheetHidden.postValue(true);
    }

    @Override
    public void onBottomSheetClick() {
        getView().setBottomSheetExpanded();
    }

    @Override
    public void onMenuCenterDeviceClicked() {
        setViewModeDevice();
    }


    @Override
    public void onClearContactClicked() {
        MessageClear m = new MessageClear();
        if(activeContact != null) {
            m.setTopic(activeContact.getId());
            messageProcessor.sendMessage(m);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.FusedContactAdded e) {
        onEvent(e.getContact());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.FusedContactRemoved c) {
        if(c.getContact() == activeContact) {
            clearActiveContact();
            setViewModeFree();
        }
        getView().removeMarker(c.getContact());

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FusedContact c) {
        getView().updateMarker(c);
        if(c == activeContact) {
            liveContact.postValue(c);
            liveCamera.postValue(c.getLatLng());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ModeChanged e) {
        getView().clearMarkers();
        clearActiveContact();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.MonitoringChanged e) {
        getView().updateMonitoringModeMenu();
    }


    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1, sticky = true)
    public void onEvent(@NonNull Location l) {
        Timber.v("location source updated");

        this.mLocation = l;
        if (mListener != null) {
            this.mListener.onLocationChanged(this.mLocation);
        }
        if(mode == VIEW_DEVICE) {
            //noinspection ConstantConditions
            liveCamera.postValue(getCurrentLocation());
        }
        getView().enableLocationMenus();
    }

    // Map Callback
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
       Timber.v("location source activated");
       mListener = onLocationChangedListener;
       if (mLocation != null)
           this.mListener.onLocationChanged(mLocation);
    }

    // Map Callback
    @Override
    public void deactivate() {
        mListener = null;
    }

    // Map Callback
    @Override
    public void onMapClick(LatLng latLng) {
        setViewModeFree();
    }

    // Map Callback
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() != null) {
            setViewModeContact(String.class.cast(marker.getTag()), false);
        }
        return true;
    }

    @Override
    public void onBottomSheetLongClick() {
        setViewModeContact(activeContact.getId(), true);
    }

}
