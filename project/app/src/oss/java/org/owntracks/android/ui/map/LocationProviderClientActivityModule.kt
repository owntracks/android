package org.owntracks.android.ui.map

import android.app.Activity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import org.owntracks.android.location.AospLocationProviderClient
import org.owntracks.android.location.LocationProviderClient

@InstallIn(ActivityComponent::class)
@Module
class LocationProviderClientActivityModule {
  @Provides
  @ActivityScoped
  fun getLocationProviderClient(activity: Activity): LocationProviderClient =
      AospLocationProviderClient(activity)
}
