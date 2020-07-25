package org.owntracks.android.injection.modules.android;

import org.owntracks.android.injection.modules.android.ActivityModules.ConnectionActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.ContactsActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.EditorActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.LoadActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.MapActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.PreferencesActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.RegionActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.RegionsActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.StatusActivityModule;
import org.owntracks.android.injection.modules.android.ActivityModules.WelcomeActivityModule;
import org.owntracks.android.injection.modules.android.ServiceModules.BackgroundServiceModule;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerReceiver;
import org.owntracks.android.injection.scopes.PerService;
import org.owntracks.android.support.receiver.StartBackgroundServiceReceiver;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class AndroindBindingModule {
    @PerActivity
    @ContributesAndroidInjector(modules = {ContactsActivityModule.class})
    abstract org.owntracks.android.ui.contacts.ContactsActivity bindContactsActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {MapActivityModule.class})
    abstract org.owntracks.android.ui.map.MapActivity bindMapActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {PreferencesActivityModule.class})
    abstract org.owntracks.android.ui.preferences.PreferencesActivity bindPreferencesActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {ConnectionActivityModule.class})
    abstract org.owntracks.android.ui.preferences.connection.ConnectionActivity bindConnectionActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {EditorActivityModule.class})
    abstract org.owntracks.android.ui.preferences.editor.EditorActivity bindEditorActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {LoadActivityModule.class})
    abstract org.owntracks.android.ui.preferences.load.LoadActivity bindLoadActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {StatusActivityModule.class})
    abstract org.owntracks.android.ui.status.StatusActivity bindStatusActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {WelcomeActivityModule.class})
    abstract org.owntracks.android.ui.welcome.WelcomeActivity bindWelcomeActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {RegionsActivityModule.class})
    abstract org.owntracks.android.ui.regions.RegionsActivity bindRegionsActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {RegionActivityModule.class})
    abstract org.owntracks.android.ui.region.RegionActivity bindRegionActivity();

    @PerService
    @ContributesAndroidInjector(modules = {BackgroundServiceModule.class})
    abstract org.owntracks.android.services.BackgroundService bindBackgroundService();

    @PerReceiver
    @ContributesAndroidInjector
    abstract StartBackgroundServiceReceiver bindBackgroundServiceReceiver();
}
