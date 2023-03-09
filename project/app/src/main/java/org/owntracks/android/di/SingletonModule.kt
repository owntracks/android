package org.owntracks.android.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.owntracks.android.ui.AppShortcuts
import org.owntracks.android.ui.AppShortcutsImpl

@InstallIn(SingletonComponent::class)
@Module
class SingletonModule {

    @Provides
    @Singleton
    fun provideOutgoingQueueIdlingResource(): CountingIdlingResource {
        return CountingIdlingResource("outgoingQueueIdlingResource", false)
    }

    @Provides
    @Singleton
    fun provideAppShortcuts(): AppShortcuts {
        return AppShortcutsImpl()
    }

    @ApplicationScope
    @Provides
    fun providesCoroutineScope(
        @CoroutineScopes.DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + defaultDispatcher)
    }

    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManagerCompat = NotificationManagerCompat.from(context)

    @Provides
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
}
