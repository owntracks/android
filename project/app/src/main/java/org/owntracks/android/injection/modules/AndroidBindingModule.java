package org.owntracks.android.injection.modules;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerReceiver;
import org.owntracks.android.injection.scopes.PerService;
import org.owntracks.android.services.BackgroundServiceModule;
import org.owntracks.android.support.receiver.StartBackgroundServiceReceiver;
import org.owntracks.android.ui.contacts.ContactsActivityModule;
import org.owntracks.android.ui.map.MapActivityModule;
import org.owntracks.android.ui.preferences.PreferencesActivityModule;
import org.owntracks.android.ui.preferences.about.AboutActivity;
import org.owntracks.android.ui.preferences.about.AboutActivityModule;
import org.owntracks.android.ui.preferences.connection.ConnectionActivityModule;
import org.owntracks.android.ui.preferences.editor.EditorActivityModule;
import org.owntracks.android.ui.preferences.load.LoadActivityModule;
import org.owntracks.android.ui.region.RegionActivityModule;
import org.owntracks.android.ui.regions.RegionsActivityModule;
import org.owntracks.android.ui.status.LogViewerActivity;
import org.owntracks.android.ui.status.LogViewerActivityModule;
import org.owntracks.android.ui.welcome.StatusActivityModule;
import org.owntracks.android.ui.welcome.WelcomeActivityModule;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class AndroidBindingModule {
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
    @ContributesAndroidInjector(modules = {AboutActivityModule.class})
    abstract AboutActivity bindAboutActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {ConnectionActivityModule.class})
    abstract org.owntracks.android.ui.preferences.connection.ConnectionActivity bindConnectionActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {LogViewerActivityModule.class})
    abstract LogViewerActivity bindLogViewerActivity();

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
