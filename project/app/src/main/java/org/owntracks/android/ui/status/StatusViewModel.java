package org.owntracks.android.ui.status;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;

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

import timber.log.Timber;


@PerActivity
public class StatusViewModel extends BaseViewModel<StatusMvvm.View> implements StatusMvvm.ViewModel<StatusMvvm.View> {
    private MessageProcessor.EndpointState endpointState;
    private String endpointMessage;

    private Date serviceStarted;
    private long locationUpdated;
    private boolean locationPermission;
    private int queueLength;

    @Inject
    public StatusViewModel(@AppContext Context context) {

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
        return queueLength;
    }

    @Override
    @Bindable
    public Date getServiceStarted() {
        return serviceStarted;
    }

    @Override
    public boolean getDozeWhitelisted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ((PowerManager) App.getContext().getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(App.getContext().getPackageName());
    }

    @Override
    @Bindable
    public long getLocationUpdated() {
        return locationUpdated;
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageProcessor.EndpointState e) {
        this.endpointState = e;
        this.endpointMessage = e.getMessage();
        notifyPropertyChanged(BR.endpointState);
        notifyPropertyChanged(BR.endpointMessage);
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

    @Subscribe(sticky = true)
    public void onEvent(Events.QueueChanged e) {
        Timber.v("queue changed %s", e.getNewLength());
        this.queueLength = e.getNewLength();
        notifyPropertyChanged(BR.endpointQueue);
    }

}
