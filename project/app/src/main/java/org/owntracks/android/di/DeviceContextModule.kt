package org.owntracks.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.location.profiles.AndroidDeviceContextProvider
import org.owntracks.android.location.profiles.DeviceContextProvider

/** On its own so that tests can replace where the device context comes from */
@InstallIn(SingletonComponent::class)
@Module
abstract class DeviceContextModule {
  @Binds
  abstract fun bindDeviceContextProvider(
      androidDeviceContextProvider: AndroidDeviceContextProvider
  ): DeviceContextProvider
}
