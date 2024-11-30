package org.owntracks.android.testutils.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import org.owntracks.android.di.LocationProviderClientModule
import org.owntracks.android.location.LocationProviderClient

@TestInstallIn(
    components = [SingletonComponent::class], replaces = [LocationProviderClientModule::class])
@Module
class TestLocationProviderClientModule {
  @Provides
  @Singleton
  fun getLocationProviderClient(
      @Suppress("UNUSED_PARAMETER") @ApplicationContext applicationContext: Context
  ): LocationProviderClient = MockLocationProviderClient()
}
