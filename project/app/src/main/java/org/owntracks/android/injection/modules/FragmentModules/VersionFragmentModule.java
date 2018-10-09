package org.owntracks.android.injection.modules.FragmentModules;


import android.support.v4.app.Fragment;

import junit.runner.Version;

import org.owntracks.android.injection.modules.SupportFragmentModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentViewModel;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import dagger.Binds;
import dagger.Module;

@Module(includes = SupportFragmentModule.class)
public abstract class VersionFragmentModule {

    @Binds
    @PerFragment
    abstract Fragment bindSupportFragment(VersionFragment f);
}