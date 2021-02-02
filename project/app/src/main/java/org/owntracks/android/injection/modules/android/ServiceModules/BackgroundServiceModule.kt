package org.owntracks.android.injection.modules.android.ServiceModules

import android.app.Service
import android.content.Context
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.qualifier.ServiceContext
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.injection.scopes.PerService
import org.owntracks.android.services.BackgroundService

@Module
abstract class BackgroundServiceModule {
    @Binds
    @PerActivity
    abstract fun bindService(s: BackgroundService?): Service?

    @Binds
    @PerService
    @ServiceContext
    abstract fun bindServiceContext(service: Service): Context
}