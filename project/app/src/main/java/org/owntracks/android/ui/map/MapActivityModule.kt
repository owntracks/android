package org.owntracks.android.ui.map

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import org.owntracks.android.ui.map.osm.OSMMapFragment

@InstallIn(ActivityComponent::class)
@Module
abstract class MapActivityModule {
//    @Binds
//    @ActivityScoped
//    abstract fun bindActivity(a: MapActivity?): AppCompatActivity?

    @Binds
    @ActivityScoped
    abstract fun bindViewModel(viewModel: MapViewModel): MapMvvm.ViewModel<MapMvvm.View>

    @Binds
    @ActivityScoped
    abstract fun bindGoogleMapFragment(googleMapFragment: GoogleMapFragment): Fragment

    @Binds
    @ActivityScoped
    abstract fun bindOSMMapFragment(osmMapFragment: OSMMapFragment): Fragment
}
