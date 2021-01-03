package org.owntracks.android.ui.map

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class MapActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: MapActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: MapViewModel): MapMvvm.ViewModel<MapMvvm.View>
}
