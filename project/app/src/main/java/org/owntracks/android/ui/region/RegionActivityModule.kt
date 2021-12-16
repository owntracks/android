package org.owntracks.android.ui.region

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.BaseViewModel

@InstallIn(ActivityComponent::class)
@Module
abstract class RegionActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: RegionViewModel): BaseViewModel<MvvmView>
}