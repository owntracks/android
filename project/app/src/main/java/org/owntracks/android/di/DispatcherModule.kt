package org.owntracks.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@InstallIn(SingletonComponent::class)
@Module
object DispatcherModule {
  @Provides
  @CoroutineScopes.DefaultDispatcher
  fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

  @Provides
  @CoroutineScopes.IoDispatcher
  fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

  @Provides
  @CoroutineScopes.MainDispatcher
  fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
