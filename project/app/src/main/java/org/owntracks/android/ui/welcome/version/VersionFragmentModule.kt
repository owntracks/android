package org.owntracks.android.ui.welcome.version

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerFragment

@Module
abstract class VersionFragmentModule {
    @Binds
    @PerFragment
    abstract fun bindSupportFragment(f: VersionFragment?): Fragment?
}