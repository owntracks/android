package org.owntracks.android.testutils.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named
import org.owntracks.android.di.GeofencingCapabilityModule

/**
 * Locations in tests come from [MockLocationProviderClient], which the OS's native geofencing (on
 * gms) never sees, so a waypoint transition could never fire there. Report native geofencing as
 * unavailable so both flavors use the app's own location-based region detection instead.
 */
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [GeofencingCapabilityModule::class],
)
@Module
object TestGeofencingCapabilityModule {
  @Provides
  @Named("nativeGeofencingAvailable")
  fun provideNativeGeofencingAvailable(): Boolean = false
}
