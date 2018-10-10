package org.owntracks.android.injection.modules.FragmentModules;

import org.owntracks.android.injection.modules.FragmentModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.preferences.PreferencesFragment;
import org.owntracks.android.ui.preferences.PreferencesFragmentMvvm;
import org.owntracks.android.ui.preferences.PreferencesFragmentViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = FragmentModule.class)
public abstract class PreferencesFragmentModule {

    @Binds
    @PerFragment
    abstract android.app.Fragment bindFragment(PreferencesFragment f);

    @Binds abstract PreferencesFragmentMvvm.ViewModel bindViewModel(PreferencesFragmentViewModel viewModel);
}