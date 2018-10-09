package org.owntracks.android.injection.modules.FragmentModules;


import android.support.v4.app.Fragment;

import org.owntracks.android.injection.modules.FragmentModule;
import org.owntracks.android.injection.modules.SupportFragmentModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentViewModel;

import javax.inject.Named;

import dagger.Binds;
import dagger.Module;

@Module(includes = SupportFragmentModule.class)
public abstract class PlayFragmentModule {

    @Binds
    @PerFragment
    abstract Fragment bindSupportFragment(PlayFragment f);

    @Binds
    @PerFragment
    abstract PlayFragmentMvvm.ViewModel bindPlayFragmentViewModel(PlayFragmentViewModel viewModel);
}