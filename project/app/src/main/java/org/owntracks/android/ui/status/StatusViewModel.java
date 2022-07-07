package org.owntracks.android.ui.status;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.BR;
import org.owntracks.android.data.EndpointState;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.status.logs.LogViewerActivity;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityScoped;
import timber.log.Timber;


@ActivityScoped
public class StatusViewModel extends BaseViewModel<StatusMvvm.View> implements StatusMvvm.ViewModel<StatusMvvm.View> {
    private final Context context;
    private EndpointState endpointState;
    private String endpointMessage;

    private Date serviceStarted;
    private long locationUpdated;
    private int queueLength;
    private final MutableLiveData<Boolean> isDozeWhitelisted = new MutableLiveData<>();

    @Inject
    public StatusViewModel(@ApplicationContext Context context) {
        this.context = context;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull StatusMvvm.View view) {
        super.attachView(savedInstanceState, view);
    }

    @Override
    @Bindable
    public EndpointState getEndpointState() {
        return endpointState;
    }

    @Override
    @Bindable
    public String getEndpointMessage() {
        return endpointMessage;
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
    public LiveData<Boolean> getDozeWhitelisted() {
        return isDozeWhitelisted;
    }

    @Override
    public void refreshDozeModeWhitelisted() {
        isDozeWhitelisted.postValue(isIgnoringBatteryOptimizations());
    }

    private boolean isIgnoringBatteryOptimizations() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ((PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(context.getApplicationContext().getPackageName());
    }

    @Override
    @Bindable
    public long getLocationUpdated() {
        return locationUpdated;
    }

    @Subscribe(sticky = true)
    public void onEvent(EndpointState e) {
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

    public void viewLogs() {
        Intent intent = new Intent(context, LogViewerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
