package org.owntracks.android.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.espresso.idling.CountingIdlingResource
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
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.ui.AppShortcuts
import org.owntracks.android.ui.AppShortcutsImpl

@InstallIn(SingletonComponent::class)
@Module
class SingletonModule {

    /**
     * This idling resource is idled when the outgoing message queue becomes empty
     *
     * @return a [CountingIdlingResource] representing the size of the outgoing message queue
     */
    @Provides
    @Named("outgoingQueueIdlingResource")
    @Singleton
    fun provideOutgoingQueueIdlingResource(): CountingIdlingResource =
        CountingIdlingResource("outgoingQueueIdlingResource", true)

    /**
     * This idling resource is idled when a message with is published in response to a remote command. Useful for tests that
     * want to trigger a response message and then wait for the app to publish it.
     *
     * @return a [SimpleIdlingResource]
     */
    @Provides
    @Named("publishResponseMessageIdlingResource")
    @Singleton
    fun provideResponseMessageIdlingResource(): SimpleIdlingResource =
        SimpleIdlingResource("publishResponseMessageIdlingResource", false)

    @Provides
    @Named("importConfigurationIdlingResource")
    @Singleton
    fun provideImportConfigurationIdlingResource(): SimpleIdlingResource =
        SimpleIdlingResource("importConfigurationIdlingResource", false)

    @Provides
    @Singleton
    @Named("mockLocationIdlingResource")
    fun provideLocationIdlingResource(): SimpleIdlingResource = SimpleIdlingResource(
        "mockLocationIdlingResource",
        false
    )

    @Provides
    @Singleton
    fun provideAppShortcuts(): AppShortcuts = AppShortcutsImpl()

    @ApplicationScope
    @Provides
    fun providesCoroutineScope(
        @CoroutineScopes.DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManagerCompat = NotificationManagerCompat.from(context)

    @Provides
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Named("CAKeyStore")
    @Singleton
    fun privateAndroidCaKeyStore(): KeyStore {
        return KeyStore.getInstance("AndroidCAStore").apply { load(null) }
    }
}
