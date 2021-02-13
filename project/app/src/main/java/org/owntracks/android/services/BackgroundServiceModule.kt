package org.owntracks.android.services

import android.app.Service
import android.content.Context
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.qualifier.ServiceContext
import org.owntracks.android.injection.scopes.PerService

@Module
abstract class BackgroundServiceModule {
    @Binds
    @PerService
    @ServiceContext
    abstract fun bindServiceContext(service: Service): Context
}