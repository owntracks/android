package org.owntracks.android.injection.modules

import android.content.Context
import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Module
import dagger.Provides
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.App
import org.owntracks.android.EventBusIndex
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.MemoryContactsRepo
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.support.ContactImageProvider
import javax.inject.Singleton

@Module
class SingletonModule {
    @Provides
    @AppContext
    @Singleton
    fun provideContext(app: App): Context {
        return app
    }

    @Provides
    @Singleton
    fun provideEventbus(): EventBus {
        return EventBus.builder().addIndex(EventBusIndex()).sendNoSubscriberEvent(false).logNoSubscriberMessages(false).build()
    }

    @Provides
    @Singleton
    fun provideContactsRepo(eventBus: EventBus?, contactImageProvider: ContactImageProvider?): ContactsRepo {
        return MemoryContactsRepo(eventBus!!, contactImageProvider!!)
    }

    @Provides
    @Singleton
    fun provideOutgoingQueueIdlingResource(): CountingIdlingResource {
        return CountingIdlingResource("outgoingQueueIdlingResource", true)
    }
}