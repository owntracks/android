package org.owntracks.android.ui.welcome.intro;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomeIntroBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;

public class IntroFragment extends BaseFragment<UiFragmentWelcomeIntroBinding, NoOpViewModel> implements WelcomeFragmentMvvm.View {
    public static final int ID = 1;

    private static IntroFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new IntroFragment();
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentComponent().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_intro, savedInstanceState);
    }
    @Override
    public void onNextClicked() {

    }

    @Override
    public boolean canProceed() {
        return true;
    }
}
