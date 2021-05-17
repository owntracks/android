package org.owntracks.android.injection.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.injection.scopes.PerReceiver
import org.owntracks.android.injection.scopes.PerService
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.BackgroundServiceModule
import org.owntracks.android.support.receiver.StartBackgroundServiceReceiver
import org.owntracks.android.ui.contacts.ContactsActivity
import org.owntracks.android.ui.contacts.ContactsActivityModule
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.map.MapActivityModule
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.preferences.PreferencesActivityModule
import org.owntracks.android.ui.preferences.about.AboutActivity
import org.owntracks.android.ui.preferences.about.AboutActivityModule
import org.owntracks.android.ui.preferences.connection.ConnectionActivity
import org.owntracks.android.ui.preferences.connection.ConnectionActivityModule
import org.owntracks.android.ui.preferences.editor.EditorActivity
import org.owntracks.android.ui.preferences.editor.EditorActivityModule
import org.owntracks.android.ui.preferences.editor.ExportedConfigContentProvider
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.preferences.load.LoadActivityModule
import org.owntracks.android.ui.region.RegionActivity
import org.owntracks.android.ui.region.RegionActivityModule
import org.owntracks.android.ui.regions.RegionsActivity
import org.owntracks.android.ui.regions.RegionsActivityModule
import org.owntracks.android.ui.status.StatusActivity
import org.owntracks.android.ui.status.logs.LogViewerActivity
import org.owntracks.android.ui.status.logs.LogViewerActivityModule
import org.owntracks.android.ui.welcome.StatusActivityModule
import org.owntracks.android.ui.welcome.WelcomeActivity
import org.owntracks.android.ui.welcome.WelcomeActivityModule

@Module
abstract class AndroidBindingModule {
    @PerActivity
    @ContributesAndroidInjector(modules = [ContactsActivityModule::class])
    abstract fun bindContactsActivity(): ContactsActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [MapActivityModule::class])
    abstract fun bindMapActivity(): MapActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [PreferencesActivityModule::class])
    abstract fun bindPreferencesActivity(): PreferencesActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [AboutActivityModule::class])
    abstract fun bindAboutActivity(): AboutActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [ConnectionActivityModule::class])
    abstract fun bindConnectionActivity(): ConnectionActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [LogViewerActivityModule::class])
    abstract fun bindLogViewerActivity(): LogViewerActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [EditorActivityModule::class])
    abstract fun bindEditorActivity(): EditorActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [LoadActivityModule::class])
    abstract fun bindLoadActivity(): LoadActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [StatusActivityModule::class])
    abstract fun bindStatusActivity(): StatusActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [WelcomeActivityModule::class])
    abstract fun bindWelcomeActivity(): WelcomeActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [RegionsActivityModule::class])
    abstract fun bindRegionsActivity(): RegionsActivity?

    @PerActivity
    @ContributesAndroidInjector(modules = [RegionActivityModule::class])
    abstract fun bindRegionActivity(): RegionActivity?

    @PerService
    @ContributesAndroidInjector(modules = [BackgroundServiceModule::class])
    abstract fun bindBackgroundService(): BackgroundService?

    @PerReceiver
    @ContributesAndroidInjector
    abstract fun bindBackgroundServiceReceiver(): StartBackgroundServiceReceiver?

    @ContributesAndroidInjector
    @PerActivity
    abstract fun bindExportedConfigContentProvider(): ExportedConfigContentProvider?
}