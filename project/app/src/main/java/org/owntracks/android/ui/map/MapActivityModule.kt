package org.owntracks.android.ui.map

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
abstract class MapActivityModule {
    @Binds
    @ActivityScoped
    abstract fun bindViewModel(viewModel: MapViewModel): MapMvvm.ViewModel<MapMvvm.View>
}
