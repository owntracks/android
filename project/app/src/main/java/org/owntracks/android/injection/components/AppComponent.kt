package org.owntracks.android.injection.components

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import org.owntracks.android.App
import org.owntracks.android.AppContextModule
import org.owntracks.android.data.repos.ObjectboxWaypointsModule
import org.owntracks.android.injection.modules.AndroidBindingModule
import org.owntracks.android.injection.modules.SingletonModule
import org.owntracks.android.services.worker.WorkerModule
import org.owntracks.android.support.RequirementsCheckerModule
import org.owntracks.android.support.preferences.SharedPreferencesStoreModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppContextModule::class,
    SingletonModule::class,
    ObjectboxWaypointsModule::class,
    AndroidSupportInjectionModule::class,
    AndroidBindingModule::class,
    SharedPreferencesStoreModule::class,
    WorkerModule::class,
    RequirementsCheckerModule::class
])
interface AppComponent : AndroidInjector<DaggerApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun app(app: App): Builder
        fun build(): AppComponent
    }

    override fun inject(instance: DaggerApplication?)
    fun inject(app: App)
}

