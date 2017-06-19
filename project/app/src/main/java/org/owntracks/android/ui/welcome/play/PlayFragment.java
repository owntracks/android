package org.owntracks.android.ui.welcome.play;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomePlayBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;
import org.owntracks.android.ui.welcome.mode.ModeFragment;

import timber.log.Timber;

public class PlayFragment extends BaseFragment<UiFragmentWelcomePlayBinding, PlayFragmentMvvm.ViewModel> implements PlayFragmentMvvm.View {
    public static final int ID = 2;

    private static PlayFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new PlayFragment();
        return instance;
    }

    public PlayFragment() {
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
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_play, savedInstanceState);
    }


    @Override
    public WelcomeFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }
}
