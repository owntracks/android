package org.owntracks.android.ui.welcome.mode;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeModeBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

public class ModeFragment extends BaseFragment<UiWelcomeModeBinding, ModeFragmentMvvm.ViewModel> implements ModeFragmentMvvm.View {
    public static final int ID = 4;

    private static ModeFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new ModeFragment();
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this); }
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_mode, savedInstanceState);
    }

    @Override
    public ModeFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }
    @Override
    public void setActivityViewModel() {
        WelcomeMvvm.View.class.cast(getActivity()).setFragmentViewModel(viewModel);
    }

}
