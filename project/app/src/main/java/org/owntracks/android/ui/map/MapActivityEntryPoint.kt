package org.owntracks.android.ui.map

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.owntracks.android.location.LocationProviderClient

@EntryPoint
@InstallIn(ActivityComponent::class)
interface MapActivityEntryPoint {
    val fragmentFactory: MapFragmentFactory
    val locationProviderClient: LocationProviderClient
}