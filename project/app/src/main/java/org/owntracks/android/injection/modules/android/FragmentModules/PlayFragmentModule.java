package org.owntracks.android.injection.modules.android.FragmentModules;


import android.support.v4.app.Fragment;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseSupportFragmentModule.class)
public abstract class PlayFragmentModule {

    @Binds
    @PerFragment
    abstract Fragment bindSupportFragment(PlayFragment f);

    @Binds
    @PerFragment
    abstract PlayFragmentMvvm.ViewModel bindViewModel(PlayFragmentViewModel viewModel);
}