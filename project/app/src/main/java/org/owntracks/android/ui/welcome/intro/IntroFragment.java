package org.owntracks.android.ui.welcome.intro;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeIntroBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

public class IntroFragment extends BaseFragment<UiWelcomeIntroBinding, NoOpViewModel> implements WelcomeFragmentMvvm.View {
    public static final int ID = 1;

    private static IntroFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new IntroFragment();
        return instance;
    }

    public IntroFragment() {
        if(viewModel == null) { fragmentComponent().inject(this); }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this);};
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_intro, savedInstanceState);
    }

    @Override
    public void onNextClicked() {
    }

    @Override
    public boolean isNextEnabled() {
        return true;
    }
}
