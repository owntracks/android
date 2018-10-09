package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.contacts.ContactsActivity;
import org.owntracks.android.ui.contacts.ContactsMvvm;
import org.owntracks.android.ui.contacts.ContactsViewModel;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.map.MapViewModel;
import org.owntracks.android.ui.preferences.connection.ConnectionActivity;

import dagger.Binds;
import dagger.Module;

@Module(includes = ActivityModule.class)
public abstract class ContactsActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity appCompatActivity(ContactsActivity a);

    @Binds abstract ContactsMvvm.ViewModel bindViewModel(ContactsViewModel viewModel);
}