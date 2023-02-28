package org.owntracks.android.di

import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
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
    fun provideAppShortcuts(): AppShortcuts {
        return AppShortcutsImpl()
    }
}
