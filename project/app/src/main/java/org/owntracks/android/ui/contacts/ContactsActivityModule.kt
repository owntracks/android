package org.owntracks.android.ui.contacts

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class ContactsActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: ContactsActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: ContactsViewModel?): ContactsMvvm.ViewModel<*>?
}