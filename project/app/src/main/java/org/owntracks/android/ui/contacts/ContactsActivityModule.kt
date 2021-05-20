package org.owntracks.android.ui.contacts

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class ContactsActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: ContactsViewModel?): ContactsMvvm.ViewModel<*>?
}