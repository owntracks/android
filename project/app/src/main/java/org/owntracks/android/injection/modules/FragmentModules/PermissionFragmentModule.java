package org.owntracks.android.injection.modules.FragmentModules;


import android.support.v4.app.Fragment;

import org.owntracks.android.injection.modules.SupportFragmentModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentMvvm;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentViewModel;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = SupportFragmentModule.class)
public abstract class PermissionFragmentModule {

    @Binds
    @PerFragment
    abstract Fragment appCompatActivity(PermissionFragment f);

    @Binds abstract PermissionFragmentMvvm.ViewModel bindPlayFragmentViewModel(PermissionFragmentViewModel viewModel);
}