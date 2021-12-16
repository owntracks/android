package org.owntracks.android.ui.status

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.owntracks.android.ui.base.viewmodel.BaseViewModel

@InstallIn(ActivityComponent::class)
@Module
abstract class StatusActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: StatusViewModel): BaseViewModel<StatusMvvm.View>
}