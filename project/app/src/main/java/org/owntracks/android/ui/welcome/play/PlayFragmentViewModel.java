package org.owntracks.android.ui.welcome.play;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;


@PerFragment
public class PlayFragmentViewModel extends BaseViewModel<PlayFragmentMvvm.View> implements PlayFragmentMvvm.ViewModel<PlayFragmentMvvm.View> {

    private boolean fixAvailable;
    private String message;

    @Inject
    public PlayFragmentViewModel() {

    }

    @Override
    public void attachView(@Nullable Bundle savedInstanceState, @NonNull PlayFragmentMvvm.View view) {
        super.attachView(savedInstanceState, view);
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

    @Override
    @Bindable
    public String getMessage() {
        return message;
    }

    @Override
    @Bindable
    public void setMessage(String message) {
         this.message = message;
    }
}
