package org.owntracks.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
object GeofencingCapabilityModule {
  @Provides
  @Named("nativeGeofencingAvailable")
  fun provideNativeGeofencingAvailable(): Boolean = true
}
