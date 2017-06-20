package org.owntracks.android.ui.welcome.permission;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomeFinishBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;
import org.owntracks.android.ui.welcome.mode.ModeFragmentMvvm;

public class PermissionFragment extends BaseFragment<UiFragmentWelcomeFinishBinding, PermissionFragmentMvvm.ViewModel> implements PermissionFragmentMvvm.View {
    public static final int ID = 3;

    private static PermissionFragment instance;
    private int PERMISSIONS_REQUEST_CODE = 1;

    public static Fragment getInstance() {
        if(instance == null)
            instance = new PermissionFragment();

        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this); }
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_permissions, savedInstanceState);
    }


    @Override
    public PermissionFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }

    public void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void checkPermission() {
        this.viewModel.setNextEnabled(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity()) == ConnectionResult.SUCCESS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.setNextEnabled(true);
        }
    }

    @Override
    public void setActivityViewModel() {
        WelcomeMvvm.View.class.cast(getActivity()).setFragmentViewModel(viewModel);
    }

}
