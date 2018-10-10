package org.owntracks.android.injection.modules.FragmentModules;


import android.support.v4.app.Fragment;

import org.owntracks.android.injection.modules.BaseSupportFragmentModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseSupportFragmentModule.class)
public abstract class VersionFragmentModule {

    @Binds
    @PerFragment
    abstract Fragment bindSupportFragment(VersionFragment f);
}