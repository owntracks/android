package org.owntracks.android.ui.welcome.intro

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.modules.android.FragmentModules.BaseSupportFragmentModule
import org.owntracks.android.injection.scopes.PerFragment

@Module(includes = [BaseSupportFragmentModule::class])
abstract class IntroFragmentModule {
    @Binds
    @PerFragment
    abstract fun bindSupportFragment(f: IntroFragment?): Fragment?
}