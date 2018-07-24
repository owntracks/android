package org.owntracks.android.injection.components;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.injection.modules.DataModule;
import org.owntracks.android.injection.modules.FragmentModule;
import org.owntracks.android.injection.modules.NetModule;
import org.owntracks.android.injection.modules.ServiceModule;
import org.owntracks.android.injection.modules.ViewModelModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.qualifier.ActivityFragmentManager;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.preferences.PreferencesActivity;

import dagger.Component;

@PerActivity
@Component(dependencies = AppComponent.class)
public interface ServiceComponent {
    void inject(org.owntracks.android.services.BackgroundService service);
}
