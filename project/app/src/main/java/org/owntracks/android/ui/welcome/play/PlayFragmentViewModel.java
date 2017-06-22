package org.owntracks.android.ui.welcome.play;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.databinding.library.baseAdapters.BR;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentMvvm;

import javax.inject.Inject;

import timber.log.Timber;


@PerFragment
public class PlayFragmentViewModel extends BaseViewModel<PlayFragmentMvvm.View> implements PlayFragmentMvvm.ViewModel<PlayFragmentMvvm.View> {

    private boolean playServicesAvailable;
    private boolean fixAvailable;

    @Inject
    public PlayFragmentViewModel() {

    }

    @Override
    public void attachView(@NonNull PlayFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        Timber.v("attaching view");
        getView().checkAvailability();
    }

    @Override
    public void onNextClicked() {

    }

    @Override
    @Bindable
    public boolean isNextEnabled() {
        return playServicesAvailable;
    }

    @Bindable
    public void setNextEnabled(boolean enabled) {
        Timber.v("set %s", enabled);
        this.playServicesAvailable = enabled;
        notifyChange();
    }

    @Override
    public void onFixClicked() {
        getView().requestFix();
    }

    @Override
    @Bindable
    public boolean isFixAvailable() {
        return fixAvailable;
    }

    @Override
    @Bindable
    public void setFixAvailable(boolean available) {
        this.fixAvailable = available;
    }
}
