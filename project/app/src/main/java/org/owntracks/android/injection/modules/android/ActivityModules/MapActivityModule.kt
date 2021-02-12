package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.map.MapMvvm
import org.owntracks.android.ui.map.MapViewModel

@Module
abstract class MapActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: MapActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: MapViewModel): MapMvvm.ViewModel<MapMvvm.View>
}