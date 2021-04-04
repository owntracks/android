package org.owntracks.android.ui.map.osm

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerFragment

@Module
abstract class OSMMapFragmentModule {
    @Binds
    @PerFragment
    abstract fun bindSupportFragment(f: OSMMapFragment?): Fragment?
}