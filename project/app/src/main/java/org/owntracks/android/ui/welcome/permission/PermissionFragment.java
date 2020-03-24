package org.owntracks.android.ui.welcome.permission;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomePermissionsBinding;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.BaseSupportFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

import javax.inject.Inject;

public class PermissionFragment extends BaseSupportFragment<UiWelcomePermissionsBinding, PermissionFragmentMvvm.ViewModel> implements PermissionFragmentMvvm.View {
    private final int PERMISSIONS_REQUEST_CODE = 1;
    private boolean askedForPermission = false;
    @Inject
    EventBus eventBus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_permissions, savedInstanceState);
    }

    public void requestFix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (askedForPermission) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog
                            .Builder(getContext())
                            .setCancelable(true)
                            .setMessage(R.string.permissions_description)
                            .setPositiveButton("OK",
                                    (dialog, which) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE))
                            .show();
                } else {
                    Toast.makeText(this.getContext(), "Unable to proceed without location permissions.", Toast.LENGTH_SHORT).show();
                }
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        askedForPermission = true;
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            eventBus.postSticky(new Events.PermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION));
        }
        ((WelcomeMvvm.View) getActivity()).refreshNextDoneButtons();
    }

    private void checkPermission() {
        if (getContext() != null) {
            viewModel.setPermissionGranted(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        }
    }

    @Override
    public boolean isNextEnabled() {
        checkPermission();
        return viewModel.isPermissionGranted();
    }

    @Override
    public void onShowFragment() {

    }
}
