package org.owntracks.android.ui.welcome.permission;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;


@PerActivity
public class PermissionFragmentViewModel extends BaseViewModel<PermissionFragmentMvvm.View> implements PermissionFragmentMvvm.ViewModel<PermissionFragmentMvvm.View> {
    private boolean permissionGranted;

    @Inject
    PermissionFragmentViewModel() {

    }

    @Override
    public void attachView(@NonNull PermissionFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
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
