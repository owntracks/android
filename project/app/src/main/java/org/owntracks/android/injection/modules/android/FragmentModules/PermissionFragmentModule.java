package org.owntracks.android.injection.modules.android.FragmentModules;


import android.support.v4.app.Fragment;

import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentMvvm;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseSupportFragmentModule.class)
public abstract class PermissionFragmentModule {

    @Binds
    abstract Fragment bindSupportFragment(PermissionFragment f);

    @Binds abstract PermissionFragmentMvvm.ViewModel bindViewModel(PermissionFragmentViewModel viewModel);
}