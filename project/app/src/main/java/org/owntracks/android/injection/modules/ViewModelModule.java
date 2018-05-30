package org.owntracks.android.injection.modules;

import org.owntracks.android.ui.preferences.PreferencesFragmentMvvm;
import org.owntracks.android.ui.preferences.PreferencesFragmentViewModel;
import org.owntracks.android.ui.preferences.connection.ConnectionMvvm;
import org.owntracks.android.ui.preferences.connection.ConnectionViewModel;
import org.owntracks.android.ui.preferences.editor.EditorMvvm;
import org.owntracks.android.ui.preferences.editor.EditorViewModel;
import org.owntracks.android.ui.contacts.ContactsMvvm;
import org.owntracks.android.ui.contacts.ContactsViewModel;
import org.owntracks.android.ui.preferences.load.LoadMvvm;
import org.owntracks.android.ui.preferences.load.LoadViewModel;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.map.MapViewModel;
import org.owntracks.android.ui.status.StatusMvvm;
import org.owntracks.android.ui.status.StatusViewModel;
import org.owntracks.android.ui.welcome.WelcomeMvvm;
import org.owntracks.android.ui.welcome.WelcomeViewModel;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentMvvm;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentViewModel;
import org.owntracks.android.ui.welcome.play.PlayFragmentMvvm;
import org.owntracks.android.ui.welcome.play.PlayFragmentViewModel;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class ViewModelModule {

    // Activities
    @Binds abstract MapMvvm.ViewModel bindMapViewModel(MapViewModel viewModel);
    @Binds abstract ContactsMvvm.ViewModel bindContactsViewModel(ContactsViewModel viewModel);
    @Binds abstract StatusMvvm.ViewModel bindStatusViewModel(StatusViewModel viewModel);
    @Binds abstract EditorMvvm.ViewModel bindEditorViewModel(EditorViewModel viewModel);
    @Binds abstract ConnectionMvvm.ViewModel bindConnectionViewModel(ConnectionViewModel viewModel);
    @Binds abstract LoadMvvm.ViewModel bindLoadViewModel(LoadViewModel viewModel);
    @Binds abstract WelcomeMvvm.ViewModel bindWelcomeViewModel(WelcomeViewModel viewModel);

    //Fragments
    @Binds abstract PermissionFragmentMvvm.ViewModel bindPermissionFragmentViewModel(PermissionFragmentViewModel viewModel);
    @Binds abstract PlayFragmentMvvm.ViewModel bindPlayFragmentViewModel(PlayFragmentViewModel viewModel);
    @Binds abstract PreferencesFragmentMvvm.ViewModel bindPreferencesFragmentViewModel(PreferencesFragmentViewModel viewModel);
}
