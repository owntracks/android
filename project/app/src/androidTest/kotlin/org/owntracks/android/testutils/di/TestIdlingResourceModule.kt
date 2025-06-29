package org.owntracks.android.testutils.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named
import javax.inject.Singleton
import org.owntracks.android.di.IdlingResourceModule
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.test.IdlingResourceWithData
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.testutils.idlingresources.IdlingResourceWithDataImpl
import org.owntracks.android.testutils.idlingresources.TestThresholdIdlingResource

@TestInstallIn(components = [SingletonComponent::class], replaces = [IdlingResourceModule::class])
@Module
class TestIdlingResourceModule {
  /**
   * This idling resource is idled when the outgoing message queue becomes empty
   *
   * @return a [ThresholdIdlingResourceInterface] representing the size of the outgoing message
   *   queue
   */
  @Provides
  @Named("outgoingQueueIdlingResource")
  @Singleton
  fun provideOutgoingQueueIdlingResource(): ThresholdIdlingResourceInterface =
      TestThresholdIdlingResource("outgoingQueueIdlingResource")

  /**
   * Idles once the ContactsActivity has finished loading
   *
   * @return a [ThresholdIdlingResourceInterface]
   */
  @Provides
  @Singleton
  @Named("contactsActivityIdlingResource")
  fun provideContactsActivityIdlingResource(): ThresholdIdlingResourceInterface =
      TestThresholdIdlingResource("contactsActivityIdlingResource")

  @Provides
  @Singleton
  @Named("messageReceivedIdlingResource")
  fun provideLocationMessageIdlingResource(): IdlingResourceWithData<MessageBase> =
      IdlingResourceWithDataImpl("messageReceivedIdlingResource", compareBy { it.messageId })

  @Provides
  @Singleton
  @Named("waypointsEventCountingIdlingResource")
  fun provideWaypointsEventCountingIdlingResource(): ThresholdIdlingResourceInterface =
      TestThresholdIdlingResource("waypointsEventCountingIdlingResource")

  @Provides
  @Singleton
  @Named("waypointsRecyclerViewIdlingResource")
  fun provideWaypointsRecyclerViewIdlingResource(): ThresholdIdlingResourceInterface =
      TestThresholdIdlingResource("waypointsRecyclerViewIdlingResource", true)
}
