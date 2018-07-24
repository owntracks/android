package org.owntracks.android.ui.welcome.play;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;


@PerActivity
public class PlayFragmentViewModel extends BaseViewModel<PlayFragmentMvvm.View> implements PlayFragmentMvvm.ViewModel<PlayFragmentMvvm.View> {

    private boolean playServicesAvailable;
    private boolean fixAvailable;

    @Inject
    public PlayFragmentViewModel() {

    }

    @Override
    public void attachView(@NonNull PlayFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }


    @Override
    public void onFixClicked() {
        getView().requestFix();
    }

    @Override
    @Bindable
    public boolean isFixAvailable() {
        return fixAvailable;
    }

    @Override
    @Bindable
    public void setFixAvailable(boolean available) {
        this.fixAvailable = available;
    }
}
