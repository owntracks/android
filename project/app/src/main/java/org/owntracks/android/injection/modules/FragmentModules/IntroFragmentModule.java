package org.owntracks.android.injection.modules.FragmentModules;


import android.support.v4.app.Fragment;

import org.owntracks.android.injection.modules.SupportFragmentModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = SupportFragmentModule.class)
public abstract class IntroFragmentModule {

    @Binds
    @PerFragment
    abstract Fragment bindSupportFragment(IntroFragment f);
}