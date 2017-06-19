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

public class IntroFragment extends BaseFragment<UiFragmentWelcomeIntroBinding, IntroFragmentMvvm.ViewModel> implements IntroFragmentMvvm.View {
    public static final int ID = 1;

    private static IntroFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new IntroFragment();
        return instance;
    }

    public IntroFragment() {
        super();
        fragmentComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_intro, savedInstanceState);
    }

    @Override
    public WelcomeFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }
}
