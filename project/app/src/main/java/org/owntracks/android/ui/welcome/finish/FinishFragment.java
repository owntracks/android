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
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;

public class FinishFragment extends BaseFragment<UiFragmentWelcomeFinishBinding, PlayFragmentMvvm.ViewModel> implements PlayFragmentMvvm.View {
    public static final int ID = 6;

    private static FinishFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new FinishFragment();
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
        return setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_finish, savedInstanceState);
    }

    @Override
    public WelcomeFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }
}
