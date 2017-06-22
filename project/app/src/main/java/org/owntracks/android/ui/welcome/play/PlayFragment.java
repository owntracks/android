package org.owntracks.android.ui.welcome.play;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomePlayBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;
import org.owntracks.android.ui.welcome.mode.ModeFragmentMvvm;

import timber.log.Timber;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class PlayFragment extends BaseFragment<UiFragmentWelcomePlayBinding, PlayFragmentMvvm.ViewModel> implements PlayFragmentMvvm.View {
    public static final int ID = 2;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1;

    private static PlayFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new PlayFragment();
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
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_play, savedInstanceState);
    }

    @Override
    public PlayFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void setActivityViewModel() {
        WelcomeMvvm.View.class.cast(getActivity()).setFragmentViewModel(viewModel);

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


    @Override
    public void checkAvailability() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        if (result == ConnectionResult.SUCCESS) {
            viewModel.setNextEnabled(true);
            viewModel.setFixAvailable(false);
            binding.message.setText(getString(R.string.play_services_now_available));
        } else if(googleAPI.isUserResolvableError(result)){
            viewModel.setNextEnabled(false);
            viewModel.setFixAvailable(true);
            binding.message.setText(getString(R.string.play_services_not_available_recoverable));
        } else {
            viewModel.setNextEnabled(false);
            viewModel.setFixAvailable(false);
            binding.message.setText(getString(R.string.play_services_not_available_not_recoverable));
        }
    }
}
