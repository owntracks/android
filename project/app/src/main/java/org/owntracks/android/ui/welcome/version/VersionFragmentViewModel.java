package org.owntracks.android.ui.welcome.version;

import android.databinding.Bindable;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;

import javax.inject.Inject;


@PerFragment
public class VersionFragmentViewModel extends BaseViewModel<VersionFragmentMvvm.View> implements VersionFragmentMvvm.ViewModel<VersionFragmentMvvm.View> {

    @Inject
    public VersionFragmentViewModel() {

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
