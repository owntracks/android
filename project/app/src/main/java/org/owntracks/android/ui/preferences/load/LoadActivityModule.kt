package org.owntracks.android.ui.preferences.load

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class LoadActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: LoadViewModel): LoadMvvm.ViewModel<LoadMvvm.View>?
}