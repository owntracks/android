package org.owntracks.android.ui.welcome.version;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;

import javax.inject.Inject;


@PerFragment
public class VersionFragmentViewModel extends BaseViewModel<VersionFragmentMvvm.View> implements VersionFragmentMvvm.ViewModel<VersionFragmentMvvm.View> {

    @Inject
    public VersionFragmentViewModel() {

    }

    @Override
    public void attachView(@NonNull VersionFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }


    @Override
    public void onNextClicked() {

    }

    @Override
    @Bindable
    public boolean isNextEnabled() {
        return true;
    }

    @Override
    public void setNextEnabled(boolean enabled) {

    }
}
