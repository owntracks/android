package org.owntracks.android.testutils.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.di.DeviceContextModule
import org.owntracks.android.location.profiles.DeviceContextProvider

/** The device context comes from [FakeDeviceContextProvider], which tests can set */
@TestInstallIn(components = [SingletonComponent::class], replaces = [DeviceContextModule::class])
@Module
object TestDeviceContextModule {
  @Provides
  @Singleton
  fun provideFakeDeviceContextProvider(waypointsRepo: WaypointsRepo): FakeDeviceContextProvider =
      FakeDeviceContextProvider(waypointsRepo)

  @Provides
  fun provideDeviceContextProvider(fake: FakeDeviceContextProvider): DeviceContextProvider = fake
}
