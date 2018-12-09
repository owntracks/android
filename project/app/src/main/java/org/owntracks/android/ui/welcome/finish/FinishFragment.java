package org.owntracks.android.ui.welcome.finish;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeFinishBinding;
import org.owntracks.android.ui.base.BaseSupportFragment;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;


public class FinishFragment extends BaseSupportFragment<UiWelcomeFinishBinding, NoOpViewModel> implements WelcomeFragmentMvvm.View {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
