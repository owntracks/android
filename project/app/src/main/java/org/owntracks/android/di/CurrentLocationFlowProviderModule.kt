package org.owntracks.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import org.owntracks.android.ui.map.MapLocationFlow

@InstallIn(ViewModelComponent::class)
@Module
class CurrentLocationFlowProviderModule {
  @Provides
  @ViewModelScoped
  fun getCurrentLocationFlow(@ApplicationContext applicationContext: Context) =
      MapLocationFlow(applicationContext)
}
