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

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomeFinishBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragment;

import timber.log.Timber;

public class PermissionFragment extends BaseFragment<UiFragmentWelcomeFinishBinding, PermissionFragmentMvvm.ViewModel> implements PermissionFragmentMvvm.View {
    public static final int ID = 3;

    private static PermissionFragment instance;
    private int PERMISSIONS_REQUEST_CODE = 1;

    public static Fragment getInstance() {
        if(instance == null)
            instance = new PermissionFragment();
        return instance;
    }

    public PermissionFragment() {
        super();
        fragmentComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel.setPermissionGranted(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_permissions, savedInstanceState);
    }


    @Override
    public WelcomeFragmentMvvm.ViewModel getViewModel() {
        return null;
    }

    public void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.v("result");

            viewModel.setPermissionGranted(true);

        }
    }
}
