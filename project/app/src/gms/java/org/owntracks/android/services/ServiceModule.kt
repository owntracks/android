package org.owntracks.android.services

import android.app.Service
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import org.owntracks.android.gms.location.GMSLocationProviderClient
import org.owntracks.android.gms.location.geofencing.GMSGeofencingClient
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.geofencing.GeofencingClient

@InstallIn(ServiceComponent::class)
@Module
class ServiceModule {
    @Provides
    @ServiceScoped
    fun getGeofencingClient(service: Service): GeofencingClient =
        GMSGeofencingClient.create(service)

    @Provides
    @ServiceScoped
    fun getLocationProviderClient(service: Service): LocationProviderClient =
        GMSLocationProviderClient.create(service)

}