package org.owntracks.android.di

import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.EventBusIndex
import org.owntracks.android.ui.AppShortcuts
import org.owntracks.android.ui.AppShortcutsImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class SingletonModule {
    @Provides
    @Singleton
    fun provideEventbus(): EventBus {
        return EventBus.builder()
            .addIndex(EventBusIndex())
            .sendNoSubscriberEvent(false)
            .logNoSubscriberMessages(true)
            .build()
    }

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

