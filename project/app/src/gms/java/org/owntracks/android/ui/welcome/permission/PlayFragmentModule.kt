package org.owntracks.android.ui.welcome.permission

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerFragment

@Module
abstract class PlayFragmentModule {
    @Binds
    @PerFragment
    abstract fun bindSupportFragment(f: PlayFragment?): Fragment?
}