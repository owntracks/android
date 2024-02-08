package org.owntracks.android.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.ConnectException
import java.security.KeyStore
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.stream.Collectors
import javax.net.ssl.SSLHandshakeException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_DISCONNECTING
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_EXCEPTION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECTION_LOST
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SERVER_CONNECT_ERROR
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.ScheduledExecutorPingSender
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.Parser
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor
import timber.log.Timber

@OptIn(ExperimentalTime::class)
class MQTTMessageProcessorEndpoint(
    messageProcessor: MessageProcessor,
    private val endpointStateRepo: EndpointStateRepo,
    private val scheduler: Scheduler,
    private val preferences: Preferences,
    private val parser: Parser,
    private val caKeyStore: KeyStore,
    @ApplicationScope private val scope: CoroutineScope,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val applicationContext: Context
) : MessageProcessorEndpoint(messageProcessor),
    StatefulServiceMessageProcessor,
    Preferences.OnPreferenceChangeListener {
    val mqttConnectionIdlingResource: SimpleIdlingResource = SimpleIdlingResource("mqttConnection", false)
    override val modeId: ConnectionMode = ConnectionMode.MQTT
    private val connectingLock = Semaphore(1)
    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var mqttClientAndConfiguration: MqttClientAndConfiguration? = null

    private val networkChangeCallback = object : ConnectivityManager.NetworkCallback() {
        var justRegistered = true
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Timber.v("Network becomes available")
            if (!justRegistered && endpointStateRepo.endpointState.value == EndpointState.DISCONNECTED) {
                Timber.v("Currently disconnected, so attempting reconnect")
                scope.launch {
                    reconnect()
                }
            }
            justRegistered = false
        }

        override fun onLost(network: Network) {
            super.onLost(network)

            scope.launch {
                Timber.w("Default network is lost, disconnecting. Waiting for lock")
                connectingLock.withPermit {
                    disconnect()
                }
                scheduler.cancelAllTasks()
            }
        }
    }

    override fun activate() {
        Timber.v("MQTT Activate")
        preferences.registerOnPreferenceChangedListener(this)
        networkChangeCallback.justRegistered = true
        connectivityManager.registerDefaultNetworkCallback(networkChangeCallback)
        scope.launch {
            try {
                val configuration = getEndpointConfiguration()
                reconnect(configuration)
            } catch (e: ConfigurationIncompleteException) {
                when (e.cause) {
                    is MqttConnectionConfiguration.MissingHostException -> Timber.w(
                        "MQTT Configuration not complete because host is missing, cannot activate"
                    )
                    else -> Timber.w(e, "MQTT Configuration not complete, cannot activate")
                }

                endpointStateRepo.setState(EndpointState.ERROR_CONFIGURATION.withError(e))
            }
        }
    }

    override fun deactivate() {
        Timber.v("MQTT Deactivate. waiting for lock")
        preferences.unregisterOnPreferenceChangedListener(this)
        connectivityManager.unregisterNetworkCallback(networkChangeCallback)
        scope.launch {
            connectingLock.withPermit {
                disconnect()
            }.apply { Timber.v("MQTT Deactivate lock released") }
            scheduler.cancelAllTasks()
        }
    }

    private suspend fun disconnect() {
        withContext(ioDispatcher) {
            Timber.v("MQTT attempting Disconnect, waiting for lock")
            measureTime {
                mqttClientAndConfiguration?.run {
                    try {
                        if (mqttClient.isConnected) mqttClient.disconnect() else Timber.d(
                            "MQTT client already not connected"
                        )
                    } catch (e: MqttException) {
                        when (e.reasonCode.toShort()) {
                            REASON_CODE_CLIENT_ALREADY_DISCONNECTED -> Timber.d(
                                "Could not disconnect from client because already disconnected"
                            )
                            REASON_CODE_CLIENT_DISCONNECTING -> Timber.d(
                                "Could not disconnect from client because already disconnecting"
                            )
                            else -> Timber.d(e, "Could not disconnect from client. Closing")
                        }
                    }
                    endpointStateRepo.setState(EndpointState.DISCONNECTED)
                    mqttClient.close(true)
                }
            }.apply { Timber.d("MQTT disconnected in $this") }
        }
    }

    override fun getEndpointConfiguration(): MqttConnectionConfiguration {
        val configuration = preferences.toMqttConnectionConfiguration()
        configuration.validate() // Throws an exception if not valid
        return configuration
    }

    override fun onFinalizeMessage(message: MessageBase): MessageBase = message

    override suspend fun sendMessage(message: MessageBase) {
        Timber.i("Sending message [$message]")
        if (endpointStateRepo.endpointState.value != EndpointState.CONNECTED || mqttClientAndConfiguration == null) {
            throw OutgoingMessageSendingException(NotConnectedException())
        }
        message.addMqttPreferences(preferences)

        // We want to block until this completes off-thread, because we've been called sync by the outgoing message loop
        mqttClientAndConfiguration?.run {
            runBlocking {
                var sendMessageThrowable: Throwable? = null
                val handler = CoroutineExceptionHandler { _, throwable ->
                    sendMessageThrowable = throwable
                }
                val job = launch(ioDispatcher + CoroutineName("MQTT SendMessage") + handler) {
                    try {
                        Timber.d("Publishing message [$message]")
                        measureTime {
                            while (mqttClient.inFlightMessageCount >= mqttConnectionConfiguration.maxInFlight) {
                                Timber.v("Pausing to wait for inflight to drop below max")
                                delay(100.milliseconds)
                            }
                            mqttClient.publish(
                                message.topic,
                                message.toJsonBytes(parser),
                                message.qos,
                                message.retained
                            )
                                .also {
                                    Timber.v("MQTT message sent with messageId=${it.messageId}. ")
                                }
                        }.apply { Timber.i("Message [$message] dispatched in $this") }
                        messageProcessor.onMessageDelivered()
                    } catch (e: Exception) {
                        Timber.e(e, "Error publishing message [$message]")
                        when (e) {
                            is IOException -> messageProcessor.onMessageDeliveryFailedFinal(message)
                            is MqttException -> {
                                if (e.reasonCode.toShort() != MqttException.REASON_CODE_MAX_INFLIGHT) {
                                    messageProcessor.onMessageDeliveryFailed(message)
                                    reconnect(mqttConnectionConfiguration)
                                }
                                throw OutgoingMessageSendingException(e)
                            }
                            else -> {
                                messageProcessor.onMessageDeliveryFailed(message)
                                throw OutgoingMessageSendingException(e)
                            }
                        }
                    }
                }
                Timber.d("Waiting for sendmessage job for message $message to finish")
                job.join()
                sendMessageThrowable?.run {
                    throw this
                }
            }
        }
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        if (preferences.mode != ConnectionMode.MQTT || mqttClientAndConfiguration == null) {
            Timber.d("Preference changed but MQTT not activated. Ignoring.")
            return
        }
        Timber.v("MQTT preferences changed: [${properties.joinToString(",")}]")
        val propertiesWeWantToReconnectOn = listOf(
            Preferences::clientId.name,
            Preferences::deviceId.name,
            Preferences::host.name,
            Preferences::keepalive.name,
            Preferences::mqttProtocolLevel.name,
            Preferences::password.name,
            Preferences::port.name,
            Preferences::tls.name,
            Preferences::tlsClientCrt.name,
            Preferences::username.name,
            Preferences::ws.name
        )
        if (propertiesWeWantToReconnectOn.stream()
            .filter(properties::contains)
            .collect(Collectors.toSet())
            .isNotEmpty()
        ) {
            Timber.d("Reconnecting to broker because of preference change")
            scope.launch {
                try {
                    reconnect(getEndpointConfiguration())
                } catch (e: Exception) {
                    when (e) {
                        is ConfigurationIncompleteException -> endpointStateRepo.setState(
                            EndpointState.ERROR_CONFIGURATION.withError(e)
                        )
                        else -> endpointStateRepo.setState(EndpointState.ERROR.withError(e))
                    }
                }
            }
        }
    }

    private val mqttCallback = object : MqttCallbackExtended {

        override fun connectionLost(cause: Throwable) {
            when (cause) {
                is IOException -> Timber.e("Connection Lost: ${cause.message}")
                else -> Timber.e(cause, "Connection Lost")
            }
            scope.launch {
                endpointStateRepo.setState(EndpointState.DISCONNECTED)
            }
            scheduler.scheduleMqttReconnect()
        }

        override fun messageArrived(topic: String, message: MqttMessage) {
            scope.launch {
                Timber.d("Received MQTT message on $topic: message=$message")
                if (message.payload.isEmpty()) {
                    onMessageReceived(
                        MessageClear().apply {
                            this.topic = topic.replace(MessageCard.BASETOPIC_SUFFIX, "")
                        }
                    )
                } else {
                    try {
                        onMessageReceived(
                            parser.fromJson(message.payload)
                                .apply {
                                    this.topic = topic
                                    this.retained = message.isRetained
                                    this.qos = message.qos
                                }
                        )
                    } catch (e: Parser.EncryptionException) {
                        Timber.w("Enable to decrypt received message ${message.id} on $topic")
                    } catch (e: JsonMappingException) {
                        Timber.w(e, "Malformed JSON message received ${message.id} on $topic")
                    } catch (e: JsonParseException) {
                        Timber.w(e, "Malformed JSON message received ${message.id} on $topic")
                    } catch (e: InvalidFormatException) {
                        Timber.w("Malformed JSON message received ${message.id} on $topic")
                    }
                }
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            token?.run {
                Timber.v("Delivery complete messageId=$messageId topics=${this.topics.joinToString(",")}}")
            }
        }

        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Timber.v("MQTT Connection complete to $serverURI. Reconnect=$reconnect")
        }
    }

    /**
     * Creates an MQTT client and connects to the broker, then subscribes to the configured topics.
     */
    @kotlin.jvm.Throws(MqttException::class)
    private suspend fun connectToBroker(mqttConnectionConfiguration: MqttConnectionConfiguration): Result<Unit> =
        withContext(ioDispatcher) {
            Timber.v("MQTT connect to Broker")
            measureTimedValue {
                endpointStateRepo.setState(EndpointState.CONNECTING)
                try {
                    val executorService = ScheduledThreadPoolExecutor(8)
                    MqttAsyncClient(
                        mqttConnectionConfiguration.connectionString,
                        mqttConnectionConfiguration.clientId,
                        RoomMqttClientPersistence(applicationContext),
                        ScheduledExecutorPingSender(executorService),
                        executorService,
                        AndroidHighResolutionTimer()
                    )
                        .also {
                            mqttClientAndConfiguration = MqttClientAndConfiguration(it, mqttConnectionConfiguration)
                        }
                        .apply {
                            Timber.i("Connecting to ${mqttConnectionConfiguration.connectionString}")
                            connect(mqttConnectionConfiguration.getConnectOptions(applicationContext, caKeyStore))
                                .waitForCompletion()
                            Timber.i(
                                "MQTT Connected. Subscribing to ${mqttConnectionConfiguration.topicsToSubscribeTo}"
                            )
                            endpointStateRepo.setState(EndpointState.CONNECTED)
                            setCallback(mqttCallback)
                            subscribe(
                                mqttConnectionConfiguration.topicsToSubscribeTo.toTypedArray(),
                                IntArray(mqttConnectionConfiguration.topicsToSubscribeTo.size) {
                                    mqttConnectionConfiguration.subQos.value
                                }
                            ).waitForCompletion()
                            Timber.i("MQTT Subscribed")

                            messageProcessor.notifyOutgoingMessageQueue()
                            if (preferences.publishLocationOnConnect) {
                                // TODO fix the trigger here
                                messageProcessor.publishLocationMessage(MessageLocation.ReportType.DEFAULT)
                            }
                        }
                    Result.success(Unit)
                } catch (e: Exception) {
                    val errorLog = "MQTT client unable to connect to endpoint"
                    when (e) {
                        is MqttException -> {
                            when (e.reasonCode) {
                                REASON_CODE_CONNECTION_LOST.toInt() -> Timber.e(
                                    e.cause,
                                    "MQTT client unable to connect to endpoint because the connection was lost"
                                )
                                REASON_CODE_CLIENT_EXCEPTION.toInt() ->
                                    when (e.cause) {
                                        is SSLHandshakeException ->
                                            Timber.e(
                                                "$errorLog: ${(e.cause as SSLHandshakeException).message}"
                                            )
                                        else -> Timber.e(e, errorLog)
                                    }
                                REASON_CODE_SERVER_CONNECT_ERROR.toInt() -> {
                                    when (e.cause) {
                                        is ConnectException -> {
                                            Timber.e("$errorLog: ${(e.cause as ConnectException).message}")
                                        }
                                    }
                                }
                                else -> Timber.e(e, errorLog)
                            }
                        }
                        else -> {
                            Timber.e(e, errorLog)
                        }
                    }
                    endpointStateRepo.setState(EndpointState.ERROR.withError(e))
                    scheduler.scheduleMqttReconnect()
                    Result.failure(e)
                }
            }.apply { Timber.d("MQTT connection attempt completed in ${this.duration}") }
                .value
        }

    override suspend fun reconnect(): Result<Unit> =
        mqttClientAndConfiguration?.mqttConnectionConfiguration?.run {
            reconnect(this)
        } ?: run {
            reconnect(getEndpointConfiguration())
        }

    private suspend fun reconnect(mqttConnectionConfiguration: MqttConnectionConfiguration): Result<Unit> =
        connectingLock.also{ Timber.v(Exception(),"MQTT reconnect request, waiting for lock") }.withPermit {
            Timber.v("MQTT reconnect with configuration $mqttConnectionConfiguration")
            disconnect()
            try {
                mqttConnectionIdlingResource.setIdleState(false)
                connectToBroker(mqttConnectionConfiguration)
            } finally {
                mqttConnectionIdlingResource.setIdleState(true)
            }
        }

    override fun checkConnection(): Boolean {
        return mqttClientAndConfiguration?.mqttClient?.run {
            var success = false
            val token = checkPing(
                null,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        success = true
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        success = false
                    }
                }
            )
            if (token != null) {
                token.waitForCompletion()
            } else {
                Timber.w("MQTT checkPing token was null")
            }
            return success
        } ?: false
    }

    class NotConnectedException : Exception()
    data class MqttClientAndConfiguration(
        val mqttClient: MqttAsyncClient,
        val mqttConnectionConfiguration: MqttConnectionConfiguration
    )

    data class TimedValue<T>(val duration: Duration, val value: T)

    /**
     * Measure timed value. A `measureTime` but also returns the return value of the block.
     * Backported from kotlin 1.9
     *
     * @param T generic type of return value
     * @param block function to execute
     * @receiver
     * @return a [TimedValue] containing the result of the block and the duration it took
     */
    private inline fun <T> measureTimedValue(block: () -> T): TimedValue<T> {
        val mark = TimeSource.Monotonic.markNow()
        val result = block()
        return TimedValue(mark.elapsedNow(), result)
    }
}
