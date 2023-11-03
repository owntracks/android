package org.owntracks.android.services

import android.content.Context
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
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.Parser
import org.owntracks.android.support.ServiceBridge
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import timber.log.Timber

@Singleton
class MessageProcessor @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val contactsRepo: ContactsRepo,
    private val preferences: Preferences,
    private val waypointsRepo: WaypointsRepo,
    parser: Parser,
    scheduler: Scheduler,
    private val endpointStateRepo: EndpointStateRepo,
    private val serviceBridge: ServiceBridge,
    @Named("outgoingQueueIdlingResource") private val outgoingQueueIdlingResource: CountingIdlingResource,
    @Named("importConfigurationIdlingResource") private val importConfigurationIdlingResource: SimpleIdlingResource,
    @Named("CAKeyStore") private val caKeyStore: KeyStore,
    private val locationProcessorLazy: Lazy<LocationProcessor>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : Preferences.OnPreferenceChangeListener {
    private var endpoint: MessageProcessorEndpoint? = null
    private var acceptMessages = false
    private val outgoingQueue: BlockingDeque<MessageBase>
    private var dequeueAndSenderJob: Job? = null
    private var retryDelayJob: Job? = null
    private var initialized = false
    private var retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
    private val httpEndpoint: MessageProcessorEndpoint
    private val mqttEndpoint: MessageProcessorEndpoint

    init {
        outgoingQueue = BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(
            100_000,
            applicationContext.filesDir,
            parser
        )
        synchronized(outgoingQueue) {
            for (i in outgoingQueue.indices) {
                outgoingQueueIdlingResource.increment()
            }
            Timber.d("Initializing the outgoingQueueIdlingResource at ${outgoingQueue.size})")
        }
        preferences.registerOnPreferenceChangedListener(this)
        httpEndpoint = HttpMessageProcessorEndpoint(
            this,
            parser,
            preferences,
            applicationContext,
            endpointStateRepo,
            caKeyStore,
            scope,
            ioDispatcher
        )
        mqttEndpoint = MQTTMessageProcessorEndpoint(
            this,
            endpointStateRepo,
            scheduler,
            preferences,
            parser,
            caKeyStore,
            scope,
            ioDispatcher,
            applicationContext
        )
    }

    @Synchronized
    fun initialize() {
        if (!initialized) {
            Timber.d("Initializing MessageProcessor")
            endpointStateRepo.setState(EndpointState.INITIAL)
            scope.launch {
                reconnect()
                initialized = true
            }
        }
    }

    /**
     * Called either by the connection activity user button, or by receiving a RECONNECT message
     */
    suspend fun reconnect(): Result<Unit> {
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
        endpoint?.deactivate()
            .also {
                Timber.d("Destroying previous endpoint")
            }
        endpointStateRepo.setQueueLength(outgoingQueue.size)
        endpoint = when (preferences.mode) {
            ConnectionMode.HTTP -> httpEndpoint
            ConnectionMode.MQTT -> mqttEndpoint
        }

        dequeueAndSenderJob =
            scope.launch(ioDispatcher) { sendAvailableMessages() }
        endpoint?.activate()
            ?.run { acceptMessages = true }
    }

    fun queueMessageForSending(message: MessageBase) {
        if (!acceptMessages) return
        outgoingQueueIdlingResource.increment()
        Timber.d("Queueing messageId:${message.messageId}, current queueLength:${outgoingQueue.size}")
        synchronized(outgoingQueue) {
            if (!outgoingQueue.offer(message)) {
                val droppedMessage = outgoingQueue.poll()
                Timber.e("Outgoing queue full. Dropping oldest message: $droppedMessage")
                if (!outgoingQueue.offer(message)) {
                    Timber.e("Still can't put message onto the queue. Dropping: ${message.messageId}")
                }
            }
            endpointStateRepo.setQueueLength(outgoingQueue.size)
        }
    }

    // Should be on the background thread here, because we block
    private suspend fun sendAvailableMessages() {
        Timber.d("Starting outbound message loop.")
        var messageFailedLastTime = false
        var retriesToGo = 0
        while (true) {
            try {
                @Suppress("BlockingMethodInNonBlockingContext") // We're in ioDispatcher here
                val message: MessageBase = outgoingQueue.take() // <--- blocks
                Timber.d("Taken message off queue: ${message.messageId}")
                endpointStateRepo.setQueueLength(outgoingQueue.size + 1)
                // reset the retry logic if the last message succeeded
                if (!messageFailedLastTime) {
                    retriesToGo = message.numberOfRetries
                    retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
                }
                messageFailedLastTime = false
                try {
                    endpoint!!.sendMessage(message)
                } catch (e: Exception) {
                    when (e) {
                        is OutgoingMessageSendingException,
                        is ConfigurationIncompleteException -> {
                            Timber.w("Error sending message ${message.messageId}. Re-queueing")
                            synchronized(outgoingQueue) {
                                if (!outgoingQueue.offerFirst(message)) {
                                    val tailMessage = outgoingQueue.removeLast()
                                    Timber.w(
                                        "Queue full when trying to re-queue failed message. " +
                                            "Dropping last message: ${tailMessage.messageId}"
                                    )
                                    if (!outgoingQueue.offerFirst(message)) {
                                        Timber.e(
                                            "Couldn't restore failed message ${message.messageId} back onto the " +
                                                "queue, dropping: "
                                        )
                                    }
                                }
                            }
                            messageFailedLastTime = true
                            Timber.i("Waiting for $retryWait before retrying ${message.messageId}")

                            scope.launch {
                                delay(retryWait)
                            }
                                .run {
                                    retryDelayJob = this
                                    join()
                                }
                            retryWait = (retryWait * 2).coerceAtMost(SEND_FAILURE_BACKOFF_MAX_WAIT)
                            retriesToGo -= 1
                        }
                        else -> {
                            Timber.e(e, "Couldn't send message ${message.messageId}. Dropping")
                            retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT
                            messageFailedLastTime = false
                        }
                    }
                }

                if (retriesToGo <= 0) {
                    messageFailedLastTime = false
                    Timber.w("Ran out of retries for sending ${message.messageId}. Dropping")
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
        Timber.w("Exiting outgoing message loop")
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
        endpointStateRepo.setQueueLength(outgoingQueue.size)
    }

    fun onMessageDeliveryFailedFinal(messageId: String?) {
        Timber.e("Message delivery failed, not retryable. $messageId")
        endpointStateRepo.setQueueLength(outgoingQueue.size)
    }

    fun onMessageDeliveryFailed(messageId: String?) {
        Timber.e("Message delivery failed. queueLength: ${outgoingQueue.size + 1}, messageId: $messageId")
        endpointStateRepo.setQueueLength(outgoingQueue.size)
    }

    suspend fun processIncomingMessage(message: MessageBase) {
        Timber.i("Received incoming message: ${message.javaClass.simpleName} on ${message.contactKey}")
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
        }
    }

    private fun processIncomingMessage(message: MessageClear) {
        scope.launch { contactsRepo.remove(message.contactKey) }
    }

    private fun processIncomingMessage(message: MessageLocation) {
        // do not use TimeUnit.DAYS.toMillis to avoid long/double conversion issues...
        if (preferences.ignoreStaleLocations > 0 &&
            System.currentTimeMillis() - message.timestamp * 1000 >
            preferences.ignoreStaleLocations.toDouble().days.inWholeMilliseconds
        ) {
            Timber.e("discarding stale location")
        } else {
            scope.launch { contactsRepo.update(message.contactKey, message) }
        }
    }

    private fun processIncomingMessage(message: MessageCard) {
        scope.launch { contactsRepo.update(message.contactKey, message) }
    }

    private suspend fun processIncomingMessage(message: MessageCmd) {
        if (!preferences.cmd) {
            Timber.w("remote commands are disabled")
            return
        }
        if (message.modeId !== ConnectionMode.HTTP &&
            preferences.receivedCommandsTopic != message.topic
        ) {
            Timber.e("cmd message received on wrong topic")
            return
        }
        if (!message.isValidMessage()) {
            Timber.e("Invalid action message received")
            return
        }
        when (message.action) {
            CommandAction.REPORT_LOCATION -> {
                if (message.modeId !== ConnectionMode.MQTT) {
                    Timber.e("command not supported in HTTP mode: ${message.action})")
                } else {
                    serviceBridge.requestOnDemandLocationFix()
                }
            }
            CommandAction.WAYPOINTS -> locationProcessorLazy.get()
                .publishWaypointsMessage()
            CommandAction.SET_WAYPOINTS -> message.waypoints?.run {
                waypointsRepo.importFromMessage(waypoints)
            }
            CommandAction.SET_CONFIGURATION -> {
                if (!preferences.remoteConfiguration) {
                    Timber.w("Received a remote configuration command but remote config setting is disabled")
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
    }

    suspend fun publishLocationMessage(trigger: MessageLocation.ReportType) {
        locationProcessorLazy.get().publishLocationMessage(trigger)
    }

    private fun processIncomingMessage(message: MessageTransition) {
        serviceBridge.sendEventNotification(message)
    }

    fun stopSendingMessages() {
        Timber.d("Interrupting background sending thread")
        dequeueAndSenderJob?.cancel()
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        if (properties.contains("mode")) {
            acceptMessages = false
            loadOutgoingMessageProcessor()
        }
    }

    val mqttConnectionIdlingResource: IdlingResource
        get() = if (endpoint is MQTTMessageProcessorEndpoint) {
            (endpoint as MQTTMessageProcessorEndpoint?)!!.mqttConnectionIdlingResource
        } else {
            SimpleIdlingResource("alwaysIdle", true)
        }

    companion object {
        private val SEND_FAILURE_BACKOFF_INITIAL_WAIT = 1.seconds
        private val SEND_FAILURE_BACKOFF_MAX_WAIT = 2.minutes
    }
}
