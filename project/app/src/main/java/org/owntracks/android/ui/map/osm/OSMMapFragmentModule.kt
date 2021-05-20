package org.owntracks.android.ui.map.osm

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.FragmentScoped

@InstallIn(ActivityComponent::class)
@Module
abstract class OSMMapFragmentModule {
    @Binds
    @FragmentScoped
    abstract fun bindSupportFragment(f: OSMMapFragment?): Fragment?
}