package org.owntracks.android.ui.welcome.finish;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.welcome.intro.IntroFragmentMvvm;

import javax.inject.Inject;

public class FinishFragmentViewModel extends BaseViewModel<FinishFragmentMvvm.View> implements FinishFragmentMvvm.ViewModel<FinishFragmentMvvm.View> {

    @Inject
    public FinishFragmentViewModel() {
    }

    public void attachView(@NonNull FinishFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Override
    public void onNextClicked() {
        // STUB
    }

    @Override
    @Bindable
    public boolean isNextEnabled() {
        return false;
    }

    @Override
    public void setNextEnabled(boolean enabled) {
        // STUB
    }
}
