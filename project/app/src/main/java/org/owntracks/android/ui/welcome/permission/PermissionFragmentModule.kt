package org.owntracks.android.ui.welcome.permission

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module

@Module
abstract class PermissionFragmentModule {
    @Binds
    abstract fun bindSupportFragment(f: PermissionFragment?): Fragment?
}