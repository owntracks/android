package org.owntracks.android.ui.welcome.play;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomePlayBinding;
import org.owntracks.android.ui.base.BaseSupportFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

import javax.inject.Inject;

public class PlayFragment extends BaseSupportFragment<UiWelcomePlayBinding, PlayFragmentMvvm.ViewModel> implements PlayFragmentMvvm.View {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1;

    public PlayFragment() {
        if(viewModel == null) { fragmentComponent().inject(this); }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this);};
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_play, savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
            checkAvailability();
        }
    }

    @Override
    public void requestFix() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        Dialog d = googleAPI.getErrorDialog(getActivity(), result, PLAY_SERVICES_RESOLUTION_REQUEST);
        if(d != null) {
            d.show();
        } else {
            checkAvailability();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAvailability();
    }


    public void checkAvailability() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        if (result == ConnectionResult.SUCCESS) {
            WelcomeMvvm.View.class.cast(getActivity()).setNextEnabled(true);
            viewModel.setFixAvailable(false);
            binding.message.setVisibility(View.VISIBLE);
            binding.message.setText(getString(R.string.play_services_now_available));
        } else {
            WelcomeMvvm.View.class.cast(getActivity()).setNextEnabled(false);
            viewModel.setFixAvailable(true);
            binding.message.setVisibility(View.VISIBLE);
            binding.message.setText(getString(R.string.play_services_not_available));
            viewModel.setFixAvailable(googleAPI.isUserResolvableError(result));
        }
    }

    @Override
    public void onNextClicked() {

    }

    @Override
    public boolean isNextEnabled() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(App.getContext()) == ConnectionResult.SUCCESS;
    }
}
