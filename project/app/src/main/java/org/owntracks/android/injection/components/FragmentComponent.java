package org.owntracks.android.injection.components;

import org.owntracks.android.injection.modules.FragmentModule;
import org.owntracks.android.injection.modules.ViewModelModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.preferences.PreferencesFragment;

import dagger.Component;

@PerFragment
@Component(dependencies = AppComponent.class, modules = {FragmentModule.class, ViewModelModule.class})
public interface FragmentComponent {
    void inject(PreferencesFragment fragment);
}
