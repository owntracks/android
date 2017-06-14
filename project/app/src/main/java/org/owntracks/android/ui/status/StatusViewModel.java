package org.owntracks.android.ui.status;

import android.Manifest;
import android.content.Context;
import android.databinding.Bindable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


@PerActivity
public class StatusViewModel extends BaseViewModel<StatusMvvm.View> implements StatusMvvm.ViewModel<StatusMvvm.View> {
    MessageProcessor.EndpointState endpointState;
    String endpointMessage;

    Context context;
    private Date appStarted;
    private Date serviceStarted;
    private long locationUpdated;
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
        return App.getMessageProcessor().getQueueLength();
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
    public long getLocationUpdated() {
        return locationUpdated;
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageProcessor.EndpointState e) {
        this.endpointState = e;
        if(e.getError() != null)
            this.endpointMessage = e.getError().toString();
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
    public void onEvent(Events.ServiceStarted e) {
        this.serviceStarted = e.getDate();
        notifyPropertyChanged(BR.serviceStarted);
    }

    @Subscribe(sticky = true)
    public void onEvent(Location l) {
        this.locationUpdated = TimeUnit.MILLISECONDS.toSeconds(l.getTime());
        notifyPropertyChanged(BR.locationUpdated);
    }
}
