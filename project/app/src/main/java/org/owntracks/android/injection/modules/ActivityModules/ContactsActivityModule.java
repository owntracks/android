package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.BaseActivityModule;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.contacts.ContactsActivity;
import org.owntracks.android.ui.contacts.ContactsMvvm;
import org.owntracks.android.ui.contacts.ContactsViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseActivityModule.class)
public abstract class ContactsActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(ContactsActivity a);

    @Binds abstract ContactsMvvm.ViewModel bindViewModel(ContactsViewModel viewModel);
}