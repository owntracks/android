package org.owntracks.android.di

import android.content.Context
import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.BuildConfig
import org.owntracks.android.EventBusIndex
import org.owntracks.android.gms.GMSRequirementsChecker
import org.owntracks.android.support.OSSRequirementsChecker
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.RequirementsChecker
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
        return CountingIdlingResource("outgoingQueueIdlingResource", true)
    }

    @Provides
    fun provideRequirementsChecker(
        preferences: Preferences,
        @ApplicationContext context: Context
    ): RequirementsChecker {
        return when (BuildConfig.FLAVOR) {
            "gms" -> GMSRequirementsChecker(preferences, context)
            else -> OSSRequirementsChecker(preferences, context)
        }
    }
}

