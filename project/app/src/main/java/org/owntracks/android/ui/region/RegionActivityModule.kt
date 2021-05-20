package org.owntracks.android.ui.region

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class RegionActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: RegionViewModel): RegionMvvm.ViewModel<RegionMvvm.View>
}