package org.owntracks.android.ui.preferences.connection

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class ConnectionActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: ConnectionViewModel): ConnectionMvvm.ViewModel<ConnectionMvvm.View>
}
