package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.contacts.ContactsActivity
import org.owntracks.android.ui.contacts.ContactsMvvm
import org.owntracks.android.ui.contacts.ContactsViewModel

@Module(includes = [BaseActivityModule::class])
abstract class ContactsActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: ContactsActivity?): AppCompatActivity?
    @Binds
    abstract fun bindViewModel(viewModel: ContactsViewModel?): ContactsMvvm.ViewModel<*>?
}