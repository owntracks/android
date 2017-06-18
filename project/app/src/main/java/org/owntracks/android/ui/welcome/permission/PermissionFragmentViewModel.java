package org.owntracks.android.ui.welcome.permission;

import android.databinding.Bindable;
import android.view.View;

import com.android.databinding.library.baseAdapters.BR;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import timber.log.Timber;


@PerFragment
public class PermissionFragmentViewModel extends BaseViewModel<PermissionFragmentMvvm.View> implements PermissionFragmentMvvm.ViewModel<PermissionFragmentMvvm.View> {

    private boolean permissionGranted = false;

    @Inject
    public PermissionFragmentViewModel() {

    }


    @Override
    @Bindable
    public boolean getPermissionGranted() {
        return permissionGranted;
    }

    @Override
    @Bindable
    public void setPermissionGranted(boolean granted) {
        this.permissionGranted = granted;
        Timber.v("granted");
        notifyPropertyChanged(BR.nextEnabled);
        notifyChange();
    }


    @Override
    public void onFixClicked() {
        getView().requestPermission();
    }

    @Override
    public void onNextClicked() {

    }
}
