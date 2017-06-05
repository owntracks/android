package org.owntracks.android.ui.status;

import android.Manifest;
import android.content.Context;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.BR;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.util.Date;

import javax.inject.Inject;


@PerActivity
public class StatusViewModel extends BaseViewModel<StatusMvvm.View> implements StatusMvvm.ViewModel<StatusMvvm.View> {
    MessageProcessor.EndpointState endpointState;
    String endpointMessage;

    Context context;
    private Date appStarted;
    private Date serviceStarted;
    private Date locationUpdated;
    private boolean locationPermission;

    @Inject
    public StatusViewModel(@AppContext Context context) {
        this.context = context;

    }
    public void attachView(@NonNull StatusMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Override
    @Bindable
    public MessageProcessor.EndpointState getEndpointState() {
        return endpointState;
    }

    @Override
    @Bindable
    public String getEndpointMessage() {
        return endpointMessage ;
    }

    @Override
    @Bindable
    public int getEndpointQueue() {
        return 0;
    }

    @Override
    @Bindable
    public boolean getPermissionLocation() {
        return locationPermission;
    }

    @Override
    @Bindable
    public Date getServiceStarted() {
        return serviceStarted;
    }

    @Override
    @Bindable
    public Date getAppStarted() {
        return appStarted;
    }

    @Override
    @Bindable
    public Date getLocationUpdated() {
        return locationUpdated;
    }

    @Subscribe(sticky = true)
    public void onEvent(Events.EndpointStateChanged e) {
        this.endpointState = e.getState();
        if(e.getException() != null)
            this.endpointMessage = e.getException().toString();
        else
            this.endpointMessage = e.getMessage();
        notifyPropertyChanged(BR.endpointState);
        notifyPropertyChanged(BR.endpointMessage);
    }

    @Subscribe(sticky = true)
    public void onEvent(Events.PermissionGranted e) {
        if(Manifest.permission.ACCESS_FINE_LOCATION.equals(e.getPermission()))
            this.locationPermission = true;
        notifyPropertyChanged(BR.locationUpdated);
    }

    @Subscribe(sticky = true)
    public void onEvent(Events.AppStarted e) {
        this.appStarted = e.getDate();
        notifyPropertyChanged(BR.appStarted);
    }

    @Subscribe(sticky = true)
    public void onEvent(Events.ServiceStarted e) {
        this.serviceStarted = e.getDate();
        notifyPropertyChanged(BR.serviceStarted);
    }

    @Subscribe(sticky = true)
    public void onEvent(Events.CurrentLocationUpdated e) {
        this.locationUpdated = e.getDate();
        notifyPropertyChanged(BR.locationUpdated);
    }
}
