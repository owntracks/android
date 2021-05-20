package org.owntracks.android.services

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@InstallIn(ServiceComponent::class)
@Module
abstract class BackgroundServiceModule {
//    @Binds
//    @ServiceScoped
//    abstract fun bindServiceContext(service: Service): Context
}