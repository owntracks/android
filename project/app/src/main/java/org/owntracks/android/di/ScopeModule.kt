package org.owntracks.android.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

@InstallIn(SingletonComponent::class)
@Module
class ScopeModule {
  @ApplicationScope
  @Provides
  fun providesCoroutineScope(
      @CoroutineScopes.DefaultDispatcher defaultDispatcher: CoroutineDispatcher
  ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

  @Provides
  fun provideNotificationManager(@ApplicationContext context: Context): NotificationManagerCompat =
      NotificationManagerCompat.from(context)

  @Provides fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

  @Provides
  @Named("CAKeyStore")
  @Singleton
  fun privateAndroidCaKeyStore(): KeyStore {
    return KeyStore.getInstance("AndroidCAStore").apply { load(null) }
  }
}
