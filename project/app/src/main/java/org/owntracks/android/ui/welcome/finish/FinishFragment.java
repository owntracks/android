package org.owntracks.android.ui.welcome.finish;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomeFinishBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;
import org.owntracks.android.ui.welcome.intro.IntroFragmentMvvm;
import org.owntracks.android.ui.welcome.mode.ModeFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;

public class FinishFragment extends BaseFragment<UiFragmentWelcomeFinishBinding, FinishFragmentMvvm.ViewModel> implements FinishFragmentMvvm.View {
    public static final int ID = 6;

    private static FinishFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new FinishFragment();
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this); }
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_finish, savedInstanceState);
    }

    @Override
    public FinishFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void setActivityViewModel() {
        WelcomeMvvm.View.class.cast(getActivity()).setFragmentViewModel(viewModel);
    }
}
