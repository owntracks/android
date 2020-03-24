package org.owntracks.android.ui.welcome.play;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomePlayBinding;
import org.owntracks.android.ui.base.BaseSupportFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

public class PlayFragment extends BaseSupportFragment<UiWelcomePlayBinding, PlayFragmentMvvm.ViewModel> implements PlayFragmentMvvm.View {
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return setAndBindContentView(inflater, container, R.layout.ui_welcome_play, savedInstanceState);
    }

    public void onPlayServicesResolutionResult() {
        checkAvailability();
    }

    @Override
    public void requestFix() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        if (!googleAPI.showErrorDialogFragment(getActivity(), result, PLAY_SERVICES_RESOLUTION_REQUEST)) {
            Toast.makeText(this.getContext(), "Unable to update Google Play Services", Toast.LENGTH_SHORT).show();
        }
        checkAvailability();
    }

    private boolean canProceed = false;

    private void checkAvailability() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        boolean fixAvailable = false;
        String playServicesStatusMessage;
        switch (result) {
            case ConnectionResult.SUCCESS:
                canProceed = true;
                playServicesStatusMessage = getString(R.string.play_services_now_available);
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
            case ConnectionResult.SERVICE_UPDATING:
                canProceed = false;
                fixAvailable = true;
                playServicesStatusMessage = getString(R.string.play_services_update_required);
                break;
            default:
                canProceed = false;
                fixAvailable = googleAPI.isUserResolvableError(result);
                playServicesStatusMessage = getString(R.string.play_services_not_available);
                break;
        }
        viewModel.setFixAvailable(fixAvailable);
        viewModel.setMessage(playServicesStatusMessage);
        ((WelcomeMvvm.View) getActivity()).refreshNextDoneButtons();
        binding.invalidateAll();
    }

    @Override
    public boolean isNextEnabled() {
        return canProceed;
    }

    @Override
    public void onShowFragment() {
        checkAvailability();
    }
}
