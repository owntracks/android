package org.owntracks.android.injection.components;

import org.owntracks.android.injection.modules.FragmentModule;
import org.owntracks.android.injection.modules.ViewModelModule;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.preferences.PreferencesFragment;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import dagger.Component;
import dagger.multibindings.IntoMap;

@PerFragment
@Component(dependencies = AppComponent.class, modules = {FragmentModule.class, ViewModelModule.class})
public interface FragmentComponent {
    void inject(PreferencesFragment fragment);

}
