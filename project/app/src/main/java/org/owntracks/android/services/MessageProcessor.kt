package org.owntracks.android.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.util.concurrent.BlockingDeque
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes.IoDispatcher
import org.owntracks.android.model.CommandAction
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageCmd
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.model.messages.MessageUnknown
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.net.MessageProcessorEndpoint
import org.owntracks.android.net.OutgoingMessageSendingException
import org.owntracks.android.net.http.HttpMessageProcessorEndpoint
import org.owntracks.android.net.mqtt.MQTTMessageProcessorEndpoint
import org.owntracks.android.preferences.DefaultsProvider.Companion.DEFAULT_SUB_TOPIC
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.Preferences.Companion.PREFERENCES_THAT_WIPE_QUEUE_AND_CONTACTS
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.IdlingResourceWithData
import org.owntracks.android.support.Parser
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import timber.log.Timber

@Singleton
class MessageProcessor
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    private val contactsRepo: ContactsRepo,
    private val preferences: Preferences,
    private val waypointsRepo: WaypointsRepo,
    private val parser: Parser,
    private val scheduler: Scheduler,
    private val endpointStateRepo: EndpointStateRepo,
    @Named("outgoingQueueIdlingResource")
    private val outgoingQueueIdlingResource: CountingIdlingResource,
    @Named("importConfigurationIdlingResource")
    private val importConfigurationIdlingResource: SimpleIdlingResource,
    @Named("messageReceivedIdlingResource")
    private val messageReceivedIdlingResource: IdlingResourceWithData<MessageBase>,
    @Named("CAKeyStore") private val caKeyStore: KeyStore,
    private val locationProcessorLazy: Lazy<LocationProcessor>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : Preferences.OnPreferenceChangeListener {
  private var endpoint: MessageProcessorEndpoint? = null
  private var acceptMessages = false
  private val outgoingQueue: BlockingDeque<MessageBase> =
      BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(
          100_000, applicationContext.filesDir, parser)
  private var dequeueAndSenderJob: Job? = null
  private var retryDelayJob: Job? = null
  private var initialized = false
  private var retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
  private var service: BackgroundService? = null

  private val serviceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
          Timber.d("${this@MessageProcessor::class.simpleName} has connected to $name")
          this@MessageProcessor.service = (service as BackgroundService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName) {
          Timber.w("${this@MessageProcessor::class.simpleName} has disconnected from $name")
          service = null
        }
      }

  init {
    synchronized(outgoingQueue) {
      for (i in outgoingQueue.indices) {
        outgoingQueueIdlingResource.increment()
      }
      Timber.d("Initializing the outgoingQueueIdlingResource at ${outgoingQueue.size})")
    }
    preferences.registerOnPreferenceChangedListener(this)
  }

  /**
   * Initialize is called by the background service when its started. This should bind back to the
   * service, and then try and connect to the endpoint.
   */
  fun initialize() {
    if (!initialized) {
      Timber.d("Initializing MessageProcessor")
      scope.launch {
        applicationContext.bindService(
            Intent(applicationContext, BackgroundService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE)
        endpointStateRepo.setState(EndpointState.INITIAL)
        reconnect()
        initialized = true
      }
    }
  }

  /** Called either by the connection activity user button, or by receiving a RECONNECT message */
  suspend fun reconnect(): Result<Unit> {
    Timber.v("reconnect")
    return try {
      when (endpoint) {
        null -> {
          loadOutgoingMessageProcessor() // The processor should take care of the reconnect on init
          Result.success(Unit)
        }
        is MQTTMessageProcessorEndpoint -> {
          (endpoint as MQTTMessageProcessorEndpoint).reconnect()
        }
        else -> {
          Result.success(Unit)
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  val isEndpointReady: Boolean
    get() {
      try {
        if (endpoint != null) {
          endpoint!!.getEndpointConfiguration()
          return true
        }
      } catch (e: ConfigurationIncompleteException) {
        return false
      }
      return false
    }

  private fun loadOutgoingMessageProcessor() {
    Timber.d("Reloading outgoing message processor")
    endpoint?.deactivate().also { Timber.d("Destroying previous endpoint") }
    scope.launch { endpointStateRepo.setQueueLength(outgoingQueue.size) }
    endpoint = getEndpoint(preferences.mode)

    dequeueAndSenderJob = scope.launch(ioDispatcher) { sendAvailableMessages() }
    endpoint?.activate()?.run { acceptMessages = true }
  }

  private fun getEndpoint(mode: ConnectionMode): MessageProcessorEndpoint {
    Timber.v("Creating endpoint for mode: $mode")
    return when (mode) {
      ConnectionMode.MQTT ->
          MQTTMessageProcessorEndpoint(
              this,
              endpointStateRepo,
              scheduler,
              preferences,
              parser,
              caKeyStore,
              scope,
              ioDispatcher,
              applicationContext)
      ConnectionMode.HTTP ->
          HttpMessageProcessorEndpoint(
              this,
              parser,
              preferences,
              applicationContext,
              endpointStateRepo,
              caKeyStore,
              scope,
              ioDispatcher)
    }
  }

  fun queueMessageForSending(message: MessageBase) {
    if (!acceptMessages) return
    outgoingQueueIdlingResource.increment()
    Timber.d("Queueing message=$message, current queueLength:${outgoingQueue.size}")
    synchronized(outgoingQueue) {
      if (!outgoingQueue.offer(message)) {
        val droppedMessage = outgoingQueue.poll()
        Timber.e("Outgoing queue full. Dropping oldest message: $droppedMessage")
        if (!outgoingQueue.offer(message)) {
          Timber.e("Still can't put message onto the queue. Dropping: $message}")
        }
      }
      scope.launch { endpointStateRepo.setQueueLength(outgoingQueue.size) }
    }
  }

  // Should be on the background thread here, because we block
  private suspend fun sendAvailableMessages() {
    try {
      Timber.d("Starting outbound message loop.")
      var messageFailedLastTime = false
      var retriesToGo = 0
      while (true) {
        try {
          @Suppress("BlockingMethodInNonBlockingContext") // We're in ioDispatcher here
          val message: MessageBase = outgoingQueue.take() // <--- blocks
          Timber.d("Taken message off queue: $message")
          endpointStateRepo.setQueueLength(outgoingQueue.size + 1)
          // reset the retry logic if the last message succeeded
          if (!messageFailedLastTime) {
            retriesToGo = message.numberOfRetries
            retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
          }
          messageFailedLastTime = false
          try {
            endpoint!!.sendMessage(message)
            if (message !is MessageWaypoint) {
              messageReceivedIdlingResource.add(message)
            }
          } catch (e: Exception) {
            when (e) {
              is OutgoingMessageSendingException,
              is ConfigurationIncompleteException -> {
                Timber.w("Error sending message $message. Re-queueing")
                synchronized(outgoingQueue) {
                  if (!outgoingQueue.offerFirst(message)) {
                    val tailMessage = outgoingQueue.removeLast()
                    Timber.w(
                        "Queue full when trying to re-queue failed message. " +
                            "Dropping last message: $tailMessage")
                    if (!outgoingQueue.offerFirst(message)) {
                      Timber.e(
                          "Couldn't restore failed message $message back onto the " +
                              "queue, dropping: ")
                    }
                  }
                }
                messageFailedLastTime = true
                // We need to launch this delay in a new job, so that we can cancel it if we need to
                scope
                    .launch {
                      Timber.i("Waiting for $retryWait before retrying $message")
                      delay(retryWait)
                    }
                    .run {
                      Timber.v("Joining on backoff delay job for $message")
                      retryDelayJob = this
                      join()
                      Timber.d("Retry wait finished for $message. Cancelled=${isCancelled}}")
                    }
                retryWait =
                    (retryWait * 2).coerceAtMost(SEND_FAILURE_BACKOFF_MAX_WAIT).also {
                      Timber.v("Increasing failure retry wait to $it")
                    }
                retriesToGo -= 1
              }
              else -> {
                Timber.e(e, "Couldn't send message $message. Dropping")
                retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
                messageFailedLastTime = false
              }
            }
          }

          if (retriesToGo <= 0) {
            messageFailedLastTime = false
            Timber.w("Ran out of retries for sending $message. Dropping")
          }

          if (!messageFailedLastTime) {
            try {
              if (!outgoingQueueIdlingResource.isIdleNow) {
                Timber.v("Decrementing outgoingQueueIdlingResource")
                outgoingQueueIdlingResource.decrement()
              }
            } catch (e: IllegalStateException) {
              Timber.w(e, "outgoingQueueIdlingResource is invalid")
            }
          }
        } catch (e: InterruptedException) {
          Timber.w(e, "Outgoing message loop interrupted")
          break
        }
      }
    } catch (e: Exception) {
      Timber.e(e, "Outgoing message loop failed")
    }
  }

  /**
   * Resets the retry backoff timer back to the initial value, because we've most likely had a
   * reconnection event.
   */
  fun notifyOutgoingMessageQueue() {
    retryDelayJob?.run {
      cancel(CancellationException("Connectivity changed"))
      Timber.d("Resetting message send loop wait.")
      retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
    }
  }

  fun onMessageDelivered() {
    scope.launch { endpointStateRepo.setQueueLength(outgoingQueue.size) }
  }

  fun onMessageDeliveryFailedFinal(message: MessageBase) {
    scope.launch {
      Timber.e("Message delivery failed, not retryable. $message")
      endpointStateRepo.setQueueLength(outgoingQueue.size)
    }
  }

  fun onMessageDeliveryFailed(message: MessageBase) {
    scope.launch {
      Timber.e("Message delivery failed. queueLength: ${outgoingQueue.size + 1}, message=$message")
      endpointStateRepo.setQueueLength(outgoingQueue.size)
    }
  }

  fun processIncomingMessage(message: MessageBase) {
    Timber.i(
        "Received incoming message: ${message.javaClass.simpleName} on ${message.topic} with id=${message.messageId}")
    when (message) {
      is MessageClear -> {
        processIncomingMessage(message)
      }
      is MessageLocation -> {
        processIncomingMessage(message)
      }
      is MessageCard -> {
        processIncomingMessage(message)
      }
      is MessageCmd -> {
        processIncomingMessage(message)
      }
      is MessageTransition -> {
        processIncomingMessage(message)
      }
      is MessageUnknown -> {
        Timber.w("Unknown message type received")
        messageReceivedIdlingResource.remove(message)
      }
    }
  }

  private fun processIncomingMessage(message: MessageClear) {
    scope.launch {
      contactsRepo.remove(message.getContactId())
      messageReceivedIdlingResource.remove(message)
    }
  }

  private fun processIncomingMessage(message: MessageLocation) {
    // do not use TimeUnit.DAYS.toMillis to avoid long/double conversion issues...
    if (preferences.ignoreStaleLocations > 0 &&
        System.currentTimeMillis() - message.timestamp * 1000 >
            preferences.ignoreStaleLocations.toDouble().days.inWholeMilliseconds) {
      Timber.e("discarding stale location")
      messageReceivedIdlingResource.remove(message)
    } else {
      scope.launch {
        contactsRepo.update(message.getContactId(), message)
        /*
        We need to idle the selfMessageReceivedIdlingResource synchronously after we call update, because
        the update method will trigger a change event, which will cause the UI to update, which needs to happen
        before we trigger any clear all contacts events. Otherwise, we have a race, and the clear all may happen
        before the contactsRepo gets updated with this location.
         */
        messageReceivedIdlingResource.remove(message)
      }
    }
  }

  private fun processIncomingMessage(message: MessageTransition) {
    if (preferences.ignoreStaleLocations > 0 &&
        System.currentTimeMillis() - message.timestamp * 1000 >
            preferences.ignoreStaleLocations.toDouble().days.inWholeMilliseconds) {
      Timber.e("discarding stale transition")
      messageReceivedIdlingResource.remove(message)
    } else {
      scope.launch {
        contactsRepo.update(message.getContactId(), message)
        service?.sendEventNotification(message)
        messageReceivedIdlingResource.remove(message)
      }
    }
  }

  private fun processIncomingMessage(message: MessageCard) {
    scope.launch {
      contactsRepo.update(message.getContactId(), message)
      messageReceivedIdlingResource.remove(message)
    }
  }

  private fun processIncomingMessage(message: MessageCmd) {
    if (!preferences.cmd) {
      Timber.w("remote commands are disabled")
      messageReceivedIdlingResource.remove(message)
    } else if (message.modeId !== ConnectionMode.HTTP &&
        preferences.receivedCommandsTopic != message.topic &&
        preferences.subTopic ==
            DEFAULT_SUB_TOPIC // If we're not using the default subtopic, we receive commands from
    // anywhere
    ) {
      Timber.e("cmd message received on wrong topic")
      messageReceivedIdlingResource.remove(message)
    } else if (!message.isValidMessage()) {
      Timber.e("Invalid action message received")
      messageReceivedIdlingResource.remove(message)
    } else {
      scope.launch {
        when (message.action) {
          CommandAction.REPORT_LOCATION -> {
            service?.requestOnDemandLocationUpdate(MessageLocation.ReportType.RESPONSE)
          }
          CommandAction.WAYPOINTS -> locationProcessorLazy.get().publishWaypointsMessage()
          CommandAction.SET_WAYPOINTS ->
              message.waypoints?.run { waypointsRepo.importFromMessage(waypoints) }
          CommandAction.SET_CONFIGURATION -> {
            if (!preferences.remoteConfiguration) {
              Timber.w(
                  "Received a remote configuration command but remote config setting is disabled")
            } else {
              if (message.configuration != null) {
                preferences.importConfiguration(message.configuration!!)
              } else {
                Timber.i("No remote configuration provided")
              }
              if (message.waypoints != null) {
                waypointsRepo.importFromMessage(message.waypoints!!.waypoints)
              } else {
                Timber.d("No remote waypoints provided")
              }
            }
            importConfigurationIdlingResource.setIdleState(true)
          }
          CommandAction.CLEAR_WAYPOINTS -> {
            waypointsRepo.clearAll()
          }
          null -> {}
        }
        messageReceivedIdlingResource.remove(message)
      }
    }
  }

  suspend fun publishLocationMessage(trigger: MessageLocation.ReportType) {
    locationProcessorLazy.get().publishLocationMessage(trigger)
  }

  fun stopSendingMessages() {
    Timber.d("Interrupting background sending thread")
    dequeueAndSenderJob?.cancel()
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (properties.intersect(PREFERENCES_THAT_WIPE_QUEUE_AND_CONTACTS).isNotEmpty()) {
      acceptMessages = false
      Timber.v("Preferences changed: [${properties.joinToString(",")}] riggering queue wipe")
      outgoingQueue.also { Timber.i("Clearing outgoing message queue length=${it.size}") }.clear()
      while (!outgoingQueueIdlingResource.isIdleNow) {
        Timber.v("Decrementing outgoingQueueIdlingResource")
        outgoingQueueIdlingResource.decrement()
      }
      loadOutgoingMessageProcessor()
    }
  }

  val mqttConnectionIdlingResource: IdlingResource
    get() =
        if (endpoint is MQTTMessageProcessorEndpoint) {
          (endpoint as MQTTMessageProcessorEndpoint?)!!.mqttConnectionIdlingResource
        } else {
          SimpleIdlingResource("alwaysIdle", true)
        }

  companion object {
    private val SEND_FAILURE_BACKOFF_INITIAL_WAIT = 1.seconds
    private val SEND_FAILURE_BACKOFF_MAX_WAIT = 2.minutes
  }
}
