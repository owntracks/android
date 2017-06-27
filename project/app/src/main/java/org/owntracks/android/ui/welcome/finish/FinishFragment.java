package org.owntracks.android.ui.welcome.finish;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeFinishBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

public class FinishFragment extends BaseFragment<UiWelcomeFinishBinding, NoOpViewModel> implements WelcomeFragmentMvvm.View {
    public static final int ID = 6;

    private static FinishFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new FinishFragment();
        return instance;
    }

    public FinishFragment() {
        if(viewModel == null) { fragmentComponent().inject(this); }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this);};

        return setAndBindContentView(inflater, container, R.layout.ui_welcome_finish, savedInstanceState);
    }


    @Override
    public void onNextClicked() {
    }

    @Override
    public boolean isNextEnabled() {
        return false;
    }
}
