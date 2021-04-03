package org.owntracks.android.ui.map

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.injection.scopes.PerFragment

@Module
abstract class MapActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: MapActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: MapViewModel): MapMvvm.ViewModel<MapMvvm.View>


    @PerFragment
    @ContributesAndroidInjector(modules = [GoogleMapFragmentModule::class])
    abstract fun bindGoogleMapFragment(): GoogleMapFragment?
}
