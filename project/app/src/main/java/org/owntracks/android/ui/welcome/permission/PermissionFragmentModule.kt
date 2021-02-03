package org.owntracks.android.ui.welcome.permission

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.modules.android.FragmentModules.BaseSupportFragmentModule

@Module(includes = [BaseSupportFragmentModule::class])
abstract class PermissionFragmentModule {
    @Binds
    abstract fun bindSupportFragment(f: PermissionFragment?): Fragment?
}