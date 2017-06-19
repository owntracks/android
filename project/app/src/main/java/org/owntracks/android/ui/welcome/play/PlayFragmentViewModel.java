package org.owntracks.android.ui.welcome.play;

import android.databinding.Bindable;

import com.android.databinding.library.baseAdapters.BR;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentMvvm;

import javax.inject.Inject;

import timber.log.Timber;


@PerFragment
public class PlayFragmentViewModel extends BaseViewModel<PlayFragmentMvvm.View> implements PlayFragmentMvvm.ViewModel<PlayFragmentMvvm.View> {

    @Inject
    public PlayFragmentViewModel() {

    }

    @Override
    public void onNextClicked() {

    }

    @Override
    @Bindable
    public boolean isNextEnabled() {
        return true;
    }
}
