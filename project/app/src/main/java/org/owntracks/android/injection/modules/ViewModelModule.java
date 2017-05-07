package org.owntracks.android.injection.modules;

import org.owntracks.android.ui.configuration.ConfigurationMvvm;
import org.owntracks.android.ui.configuration.ConfigurationViewModel;
import org.owntracks.android.ui.contacts.ContactsMvvm;
import org.owntracks.android.ui.contacts.ContactsViewModel;
import org.owntracks.android.ui.load.LoadMvvm;
import org.owntracks.android.ui.load.LoadViewModel;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.map.MapViewModel;
import org.owntracks.android.ui.status.StatusMvvm;
import org.owntracks.android.ui.status.StatusViewModel;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class ViewModelModule {

    // Activities
    @Binds
    abstract MapMvvm.ViewModel bindMapViewModel(MapViewModel mapViewModel);

    @Binds
    abstract ContactsMvvm.ViewModel bindContactsViewModel(ContactsViewModel contactsViewModel);

    @Binds
    abstract StatusMvvm.ViewModel bindStatusViewModel(StatusViewModel statusViewModel);

    @Binds
    abstract ConfigurationMvvm.ViewModel bindConfigurationViewModel(ConfigurationViewModel statusViewModel);

    @Binds
    abstract LoadMvvm.ViewModel bindLoadViewModel(LoadViewModel statusViewModel);

}
