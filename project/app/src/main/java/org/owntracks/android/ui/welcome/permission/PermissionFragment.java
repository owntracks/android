package org.owntracks.android.ui.welcome.permission;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomePermissionsBinding;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

public class PermissionFragment extends BaseFragment<UiWelcomePermissionsBinding, PermissionFragmentMvvm.ViewModel> implements PermissionFragmentMvvm.View {
    public static final int ID = 3;

    private static PermissionFragment instance;
    private final int PERMISSIONS_REQUEST_CODE = 1;

    public static Fragment getInstance() {
        if(instance == null)
            instance = new PermissionFragment();

        return instance;
    }

    public PermissionFragment() {
        if(viewModel == null) { fragmentComponent().inject(this); }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this);};
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_permissions, savedInstanceState);
    }

    public void requestFix() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            App.getEventBus().post(new Events.PermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION));
        }
        checkPermission();
    }

    public void checkPermission() {
        viewModel.setPermissionGranted(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        WelcomeMvvm.View.class.cast(getActivity()).setNextEnabled(viewModel.isPermissionGranted());

    }

    @Override
    public void onNextClicked() {

    }

    @Override
    public boolean isNextEnabled() {
        return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
