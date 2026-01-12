package org.owntracks.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import org.owntracks.android.data.repos.RoomBackedMessageQueue
import org.owntracks.android.di.CoroutineScopes.IoDispatcher
import org.owntracks.android.model.Parser

@InstallIn(SingletonComponent::class)
@Module
object MessageQueueModule {
  private const val QUEUE_CAPACITY = 100_000

  @Provides
  @Singleton
  fun provideMessageQueue(
      @ApplicationContext applicationContext: Context,
      parser: Parser,
      @IoDispatcher ioDispatcher: CoroutineDispatcher
  ): RoomBackedMessageQueue {
    return RoomBackedMessageQueue(QUEUE_CAPACITY, applicationContext, parser, ioDispatcher)
  }
}
