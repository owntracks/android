package org.owntracks.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.owntracks.android.gms.location.GMSLocationProviderClient
import org.owntracks.android.location.ExternalGnssLocationProviderClient
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.external.ExternalGnssController

@InstallIn(SingletonComponent::class)
@Module
class LocationProviderClientModule {
  @Provides
  @Singleton
  fun getLocationProviderClient(
      @ApplicationContext applicationContext: Context,
      externalGnssController: ExternalGnssController,
  ): LocationProviderClient =
      ExternalGnssLocationProviderClient(
          delegate = GMSLocationProviderClient.create(applicationContext),
          controller = externalGnssController,
      )
}
