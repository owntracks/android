package org.owntracks.android.injection.modules

import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Module
import dagger.Provides
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.EventBusIndex
import javax.inject.Singleton

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
        return CountingIdlingResource("outgoingQueueIdlingResource", true)
    }
}