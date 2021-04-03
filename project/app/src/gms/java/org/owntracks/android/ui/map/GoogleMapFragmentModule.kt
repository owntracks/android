package org.owntracks.android.ui.map

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerFragment

@Module
abstract class GoogleMapFragmentModule {
    @Binds
    @PerFragment
    abstract fun bindSupportFragment(f: GoogleMapFragment?): Fragment?
}