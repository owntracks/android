package org.owntracks.android.ui.welcome.permission;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import timber.log.Timber;


@PerFragment
public class PermissionFragmentViewModel extends BaseViewModel<PermissionFragmentMvvm.View> implements PermissionFragmentMvvm.ViewModel<PermissionFragmentMvvm.View> {
    boolean permissionGranted;

    @Inject
    PermissionFragmentViewModel() {

    }

    @Override
    public void attachView(@NonNull PermissionFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        getView().checkPermission();
    }

    @Override
    public void onFixClicked() {
        getView().requestFix();
    }

    public boolean isPermissionGranted() {
        return permissionGranted;
    }

    public void setPermissionGranted(boolean permissionGranted) {
        this.permissionGranted = permissionGranted;
        notifyChange();
    }
}
