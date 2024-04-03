package org.owntracks.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import org.owntracks.android.location.NoopGeofencingClient
import org.owntracks.android.location.geofencing.GeofencingClient

@InstallIn(ServiceComponent::class)
@Module
class ServiceModule {
  @Provides @ServiceScoped fun getGeofencingClient(): GeofencingClient = NoopGeofencingClient()
}
