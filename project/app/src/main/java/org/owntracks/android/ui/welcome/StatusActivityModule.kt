package org.owntracks.android.ui.welcome

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.owntracks.android.ui.status.StatusMvvm
import org.owntracks.android.ui.status.StatusViewModel

@InstallIn(ActivityComponent::class)
@Module
abstract class StatusActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: StatusViewModel): StatusMvvm.ViewModel<StatusMvvm.View>
}