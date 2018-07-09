package org.owntracks.android.injection.components;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.modules.ViewModelModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.qualifier.ActivityFragmentManager;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.preferences.PreferencesActivity;

import dagger.Component;

@PerActivity
@Component(dependencies = AppComponent.class, modules = {ActivityModule.class, ViewModelModule.class})
public interface ActivityComponent {
    @ActivityContext Context activityContext();
    @ActivityFragmentManager FragmentManager activityFragmentManager();

    void inject(org.owntracks.android.ui.contacts.ContactsActivity activity);
    void inject(org.owntracks.android.ui.map.MapActivity activity);
    void inject(org.owntracks.android.ui.preferences.PreferencesActivity activity);
    void inject(org.owntracks.android.ui.preferences.connection.ConnectionActivity activity);
    void inject(org.owntracks.android.ui.preferences.editor.EditorActivity activity);
    void inject(org.owntracks.android.ui.preferences.load.LoadActivity activity);
    void inject(org.owntracks.android.ui.status.StatusActivity activity);
    void inject(org.owntracks.android.ui.welcome.WelcomeActivity activity);
    void inject(org.owntracks.android.ui.region.RegionActivity activity);
    void inject(org.owntracks.android.ui.regions.RegionsActivity activity);
    void inject(org.owntracks.android.ui.regions.RoomRegionsActivity activity);
    void inject(org.owntracks.android.ui.regions.RoomRegionActivity activity);

}
