package org.owntracks.android.ui.welcome.mode;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeModeBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;
import org.owntracks.android.ui.welcome.finish.FinishFragment;

public class ModeFragment extends BaseFragment<UiWelcomeModeBinding, ModeFragmentMvvm.ViewModel> implements ModeFragmentMvvm.View {
    public static final int ID = 4;

    private static ModeFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new ModeFragment();
        return instance;
    }

    public ModeFragment() {
        if(viewModel == null) { fragmentComponent().inject(this); }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this);};
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_mode, savedInstanceState);
    }

    @Override
    public void onNextClicked() {
        viewModel.onNextClicked();
    }

    @Override
    public boolean isNextEnabled() {
        return true;
    }
}
