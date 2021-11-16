package org.owntracks.android.ui

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.owntracks.android.ui.map.IMapFragmentFactory
import org.owntracks.android.ui.map.MapFragmentFactory

@InstallIn(ActivityComponent::class)
@Module
abstract class MapFragmentModule {
    @Binds
    abstract fun bindMapFragmentFactory(mapFragmentFactory: MapFragmentFactory): IMapFragmentFactory
}