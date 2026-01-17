package org.owntracks.android.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.data.repos.RoomBackedMessageQueue
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes.IoDispatcher
import org.owntracks.android.model.CommandAction
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageCmd
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.model.messages.MessageUnknown
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.net.MessageProcessorEndpoint
import org.owntracks.android.net.http.HttpMessageProcessorEndpoint
import org.owntracks.android.net.mqtt.MQTTMessageProcessorEndpoint
import org.owntracks.android.preferences.DefaultsProvider.Companion.DEFAULT_SUB_TOPIC
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.Preferences.Companion.PREFERENCES_THAT_WIPE_QUEUE_AND_CONTACTS
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import org.owntracks.android.test.IdlingResourceWithData
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
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
    private val outgoingQueueIdlingResource: ThresholdIdlingResourceInterface,
    @Named("importConfigurationIdlingResource")
    private val importConfigurationIdlingResource: SimpleIdlingResource,
    @Named("messageReceivedIdlingResource")
    private val messageReceivedIdlingResource: IdlingResourceWithData<MessageBase>,
    @Named("CAKeyStore") private val caKeyStore: KeyStore,
    private val locationProcessorLazy: Lazy<LocationProcessor>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
    @Named("mqttConnectionIdlingResource")
    private val mqttConnectionIdlingResource: SimpleIdlingResource,
    private val outgoingQueue: RoomBackedMessageQueue
) : Preferences.OnPreferenceChangeListener {
  private var endpoint: MessageProcessorEndpoint? = null
  private val queueInitJob: Job =
      scope.launch(ioDispatcher) {
        outgoingQueue.initialize(applicationContext.filesDir)
        val initialSize = outgoingQueue.size()
        repeat(initialSize) { outgoingQueueIdlingResource.increment() }
        Timber.d("Initialized outgoingQueue with size: $initialSize")
      }
  private var dequeueAndSenderJob: Job? = null
  private var retryDelayJob: Job? = null
  private var initialized = false
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
            Context.BIND_AUTO_CREATE,
        )
        endpointStateRepo.setState(EndpointState.INITIAL)
        queueInitJob.join()
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
    runBlocking { queueInitJob.join() }
    Timber.d("Reloading outgoing message processor")
    endpoint?.deactivate().also { Timber.d("Destroying previous endpoint") }
    scope.launch { endpointStateRepo.setQueueLength(outgoingQueue.size()) }
    endpoint = getEndpoint(preferences.mode)

    dequeueAndSenderJob =
        scope.launch(ioDispatcher) {
          endpoint?.activate()
          sendAvailableMessages()
        }
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
              applicationContext,
              mqttConnectionIdlingResource,
          )
      ConnectionMode.HTTP ->
          HttpMessageProcessorEndpoint(
              this,
              parser,
              preferences,
              applicationContext,
              endpointStateRepo,
              caKeyStore,
              scope,
              ioDispatcher,
          )
    }
  }

  fun queueMessageForSending(message: MessageBase) {
    outgoingQueueIdlingResource.increment()
    scope.launch(ioDispatcher) {
      val currentSize = outgoingQueue.size()
      Timber.d("Queueing message=$message, current queueLength:$currentSize")
      if (!outgoingQueue.enqueue(message)) {
        val droppedMessage = outgoingQueue.dequeue()
        Timber.e("Outgoing queue full. Dropping oldest message: $droppedMessage")
        if (!outgoingQueue.enqueue(message)) {
          Timber.e("Still can't put message onto the queue. Dropping: $message")
        }
      }
      endpointStateRepo.setQueueLength(outgoingQueue.size())
    }
  }

  /**
   * Shows the result of the last message send operation
   *
   * @constructor Create empty Last message status
   */
  private sealed class LastMessageStatus {
    data object Success : LastMessageStatus()

    data object PermanentFailure : LastMessageStatus()

    data class RetryableFailure(val numberOfRetries: Int, val nextTimeWaitFor: Duration) :
        LastMessageStatus()
  }

  private val outboundMessageQueueMutex = Mutex(false)

  // Should be on the background thread here, because we block
  private suspend fun sendAvailableMessages() {
    if (outboundMessageQueueMutex.isLocked) {
      Timber.d("Outbound message loop already running. Skipping.")
      return
    }
    outboundMessageQueueMutex.withLock {
      try {
        Timber.d("Starting outbound message loop.")
        var lastMessageStatus: LastMessageStatus = LastMessageStatus.Success
        var retriesToGo = 0
        var retryWait: Duration
        while (true) {
          try {
            val message: MessageBase? = outgoingQueue.dequeue()
            if (message == null) {
              delay(100) // Poll every 100ms when queue is empty
              continue
            }
            Timber.d("Taken message off queue: $message")
            endpointStateRepo.setQueueLength(outgoingQueue.size() + 1)
            // reset the retry logic if the last message succeeded
            if (lastMessageStatus is LastMessageStatus.Success ||
                lastMessageStatus is LastMessageStatus.PermanentFailure) {
              retriesToGo = message.numberOfRetries
              retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
            } else {
              retryWait = (lastMessageStatus as LastMessageStatus.RetryableFailure).nextTimeWaitFor
            }
            // Let's try to send the message
            try {
              endpoint.let {
                if (it == null) {
                  Timber.i("Endpoint not ready yet. Re-queueing")
                  reQueueMessage(message)
                  resendDelayWait(SEND_FAILURE_NOT_READY_WAIT)
                  lastMessageStatus =
                      LastMessageStatus.RetryableFailure(
                          message.numberOfRetries,
                          SEND_FAILURE_BACKOFF_INITIAL_WAIT,
                      )
                } else {
                  it.sendMessage(message).exceptionOrNull()?.run {
                    when (this) {
                      is MessageProcessorEndpoint.NotReadyException -> {
                        Timber.i("Endpoint not ready yet. Re-queueing")
                        reQueueMessage(message)
                        resendDelayWait(SEND_FAILURE_NOT_READY_WAIT)
                        lastMessageStatus =
                            LastMessageStatus.RetryableFailure(
                                message.numberOfRetries,
                                SEND_FAILURE_BACKOFF_INITIAL_WAIT,
                            )
                      }

                      is MessageProcessorEndpoint.OutgoingMessageSendingException,
                      is ConfigurationIncompleteException,
                      is MQTTMessageProcessorEndpoint.NotConnectedException -> {
                        when (this) {
                          is MessageProcessorEndpoint.OutgoingMessageSendingException ->
                              Timber.w(this, "Error sending message $message. Re-queueing")
                          is ConfigurationIncompleteException ->
                              Timber.w("Configuration incomplete for message $message. Re-queueing")
                          is MQTTMessageProcessorEndpoint.NotConnectedException ->
                              Timber.w("MQTT not connected for message $message. Re-queueing")
                        }
                        reQueueMessage(message)
                        resendDelayWait(retryWait)

                        lastMessageStatus =
                            if (retriesToGo <= 0) {
                              Timber.w("Ran out of retries for sending. Dropping message")
                              LastMessageStatus.PermanentFailure
                            } else {
                              LastMessageStatus.RetryableFailure(
                                  retriesToGo - 1,
                                  (retryWait * 2).coerceAtMost(SEND_FAILURE_BACKOFF_MAX_WAIT).also {
                                    Timber.v("Increasing failure retry wait to $it")
                                  },
                              )
                            }
                      }

                      else -> {
                        Timber.e(this, "Couldn't send message $message. Dropping")
                        lastMessageStatus = LastMessageStatus.PermanentFailure
                      }
                    }
                  }
                      ?: run {
                        Timber.d("Message sent successfully: $message")
                        lastMessageStatus = LastMessageStatus.Success
                        if (message !is MessageWaypoint) {
                          messageReceivedIdlingResource.add(message)
                        }
                      }
                }
              }
            } catch (e: Exception) {
              // Who knows why we're here. We didn't plan to be.
              Timber.e(e, "Error sending message $message. Dropping.")
              lastMessageStatus = LastMessageStatus.PermanentFailure
            }

            if (lastMessageStatus is LastMessageStatus.Success ||
                lastMessageStatus is LastMessageStatus.PermanentFailure) {
              try {
                if (!outgoingQueueIdlingResource.isIdleNow) {
                  Timber.v("Decrementing outgoingQueueIdlingResource")
                  outgoingQueueIdlingResource.decrement()
                }
              } catch (e: IllegalStateException) {
                Timber.w(e, "outgoingQueueIdlingResource is invalid")
              }
            }
          } catch (e: CancellationException) {
            Timber.w(e, "Outgoing message loop cancelled")
            break
          } finally {
            endpointStateRepo.setQueueLength(outgoingQueue.size())
          }
        }
      } catch (e: Exception) {
        Timber.e(e, "Outgoing message loop failed")
      } finally {
        Timber.i("finishing outbound message loop")
      }
    }
  }

  /**
   * Launches a delay job, and then blocks waiting for it to either finish or be cancelled
   *
   * @param waitFor how long to wait for
   * @return whether or not the wait job was cancelled
   */
  private suspend fun resendDelayWait(waitFor: Duration): Boolean =
      scope
          .launch {
            Timber.i("Waiting for $waitFor before retrying send")
            delay(waitFor)
          }
          .run {
            Timber.v("Joining on backoff delay job")
            retryDelayJob = this
            measureTime { join() }
                .run { Timber.d("Retry wait finished after $this. Cancelled=${isCancelled}}") }
            return isCancelled
          }

  // Takes a message and sticks it on the head of the queue
  private suspend fun reQueueMessage(message: MessageBase) {
    if (!outgoingQueue.requeue(message)) {
      val tailMessage = outgoingQueue.dequeue()
      Timber.w(
          "Queue full when trying to re-queue failed message. " +
              "Dropping oldest message: $tailMessage",
      )
      if (!outgoingQueue.requeue(message)) {
        Timber.e(
            "Couldn't restore failed message $message back onto the queue, dropping: ",
        )
      }
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
    }
  }

  fun onMessageDeliveryFailedFinal(message: MessageBase) {
    scope.launch {
      Timber.e("Message delivery failed, not retryable. $message")
      endpointStateRepo.setQueueLength(outgoingQueue.size())
    }
  }

  fun onMessageDeliveryFailed(message: MessageBase) {
    scope.launch {
      Timber.e(
          "Message delivery failed. queueLength: ${outgoingQueue.size() + 1}, message=$message")
      endpointStateRepo.setQueueLength(outgoingQueue.size())
    }
  }

  fun processIncomingMessage(message: MessageBase) {
    Timber.d(
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
      Timber.i("Received clear message for ${message.getContactId()}")
      contactsRepo.remove(message.getContactId())
      messageReceivedIdlingResource.remove(message)
    }
  }

  private fun processIncomingMessage(message: MessageLocation) {
    // do not use TimeUnit.DAYS.toMillis to avoid long/double conversion issues...
    if (preferences.ignoreStaleLocations > 0 &&
        System.currentTimeMillis() - message.timestamp * 1000 >
            preferences.ignoreStaleLocations.toDouble().days.inWholeMilliseconds) {
      Timber.d("discarding stale location from ${message.getContactId()} at ${message.timestamp}")
      messageReceivedIdlingResource.remove(message)
    } else {
      scope.launch {
        if (message.topic == preferences.pubTopicLocations) {
          Timber.d(
              "Received our own location update ${message.latitude},${message.longitude} at ${message.timestamp}")
        } else {
          Timber.i(
              "Contact ${message.getContactId()} moved to ${message.latitude},${message.longitude} at ${message.timestamp}")
        }
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
      Timber.d("discarding stale transition from $message.topic at $message.timestamp")
      messageReceivedIdlingResource.remove(message)
    } else {
      scope.launch {
        Timber.i(
            "Contact ${message.getContactId()} transitioned waypoint ${message.description} (${message.event}) at ${message.timestamp}")
        contactsRepo.update(message.getContactId(), message)
        service?.sendEventNotification(message)
        messageReceivedIdlingResource.remove(message)
      }
    }
  }

  private fun processIncomingMessage(message: MessageCard) {
    scope.launch {
      Timber.i("Received card message from ${message.topic}")
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
            DEFAULT_SUB_TOPIC // If we're not using the default subtopic, we receive commands
    // from
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
          CommandAction.STATUS -> locationProcessorLazy.get().publishStatusMessage()
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
      Timber.v("Preferences changed: [${properties.joinToString(",")}] triggering queue wipe")
      runBlocking {
        queueInitJob.join()
        outgoingQueue.clear()
        Timber.i("Cleared outgoing message queue")
      }
      while (!outgoingQueueIdlingResource.isIdleNow) {
        Timber.v("Decrementing outgoingQueueIdlingResource")
        try {
          outgoingQueueIdlingResource.decrement()
        } catch (e: IllegalStateException) {
          Timber.w(e, "outgoingQueueIdlingResource is invalid")
        }
      }
      loadOutgoingMessageProcessor()
    }
  }

  companion object {
    private val SEND_FAILURE_NOT_READY_WAIT = 10.seconds
    private val SEND_FAILURE_BACKOFF_INITIAL_WAIT = 1.seconds
    private val SEND_FAILURE_BACKOFF_MAX_WAIT = 2.minutes
  }
}
