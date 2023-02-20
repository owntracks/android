package org.owntracks.android.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.fasterxml.jackson.core.JsonParseException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.stream.Collectors
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.di.IoDispatcher
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val applicationContext: Context
) : MessageProcessorEndpoint(messageProcessor),
    StatefulServiceMessageProcessor,
    Preferences.OnPreferenceChangeListener {
    private val connectingLock = Semaphore(1)
    private val scope = GlobalScope // TODO use serv ice lifecycle scope
    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkChangeCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (endpointStateRepo.endpointState == EndpointState.DISCONNECTED) {
                reconnect()
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Timber.w("Default network is lost, disconnecting")
            scope.launch {
                connectingLock.withPermit {
                    disconnect()
                }
                scheduler.cancelMqttTasks()
            }
        }
    }

    val mqttConnectionIdlingResource: SimpleIdlingResource = SimpleIdlingResource("mqttConnection", true)
    private var mqttClientAndConfiguration: MqttClientAndConfiguration? = null

    override fun activate() {
        Timber.v("MQTT Activate")
        preferences.registerOnPreferenceChangedListener(this@MQTTMessageProcessorEndpoint)
        connectivityManager.registerDefaultNetworkCallback(networkChangeCallback)
        scope.launch {
            try {
                val configuration = getEndpointConfiguration()
                connectingLock.withPermit {
                    mqttConnectionIdlingResource.setIdleState(false)
                    connectToBroker(configuration)
                    mqttConnectionIdlingResource.setIdleState(true)
                }
            } catch (e: ConfigurationIncompleteException) {
                Timber.w(e, "MQTT Configuration not complete, cannot activate")
                endpointStateRepo.setState(EndpointState.ERROR_CONFIGURATION.withError(e))
            }
        }
    }

    override fun deactivate() {
        Timber.v("MQTT Deactivate")
        preferences.unregisterOnPreferenceChangedListener(this@MQTTMessageProcessorEndpoint)
        connectivityManager.unregisterNetworkCallback(networkChangeCallback)
        scope.launch {
            connectingLock.withPermit {
                disconnect()
            }
            scheduler.cancelMqttTasks()
        }
    }

    private suspend fun disconnect() {
        withContext(ioDispatcher) {
            Timber.v("MQTT Disconnect")
            measureTime {
                mqttClientAndConfiguration?.run {
                    try {
                        mqttClient.disconnect()
                    } catch (e: MqttException) {
                        Timber.i("Could not disconnect from client, because ${e.message}. Closing")
                    }
                    endpointStateRepo.setState(EndpointState.DISCONNECTED)
                    mqttClient.close(true)
                }
            }.apply { Timber.d("MQTT disconnected in $this") }
        }
    }

    override fun getEndpointConfiguration(): MqttConnectionConfiguration {
        Timber.v("MQTT getEndpointConfiguration")
        val configuration = preferences.toMqttConnectionConfiguration()
        configuration.validate() // Throws an exception if not valid
        return configuration
    }

    override fun onFinalizeMessage(message: MessageBase): MessageBase = message

    override val modeId: ConnectionMode = ConnectionMode.MQTT

    override fun sendMessage(message: MessageBase) {
        Timber.i("Sending message $message")
        if (endpointStateRepo.endpointState != EndpointState.CONNECTED || mqttClientAndConfiguration == null) {
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
                val job = scope.launch(ioDispatcher + CoroutineName("MQTT SendMessage") + handler) {
                    try {
                        Timber.d("Publishing message id=${message.messageId}")
                        measureTime {
                            while (true) {
                                try {
                                    mqttClient.publish(
                                        message.topic,
                                        message.toJsonBytes(parser),
                                        message.qos,
                                        message.retained
                                    )
                                        .also { Timber.v("MQTT message sent with messageId=${it.messageId}") }
                                    break
                                } catch (e: MqttException) {
                                    if (e.reasonCode.toShort() == MqttException.REASON_CODE_MAX_INFLIGHT) {
                                        throw e
                                        // We need to try again. Bug in the MQTT client: https://github.com/eclipse/paho.mqtt.java/issues/551
                                    } else {
                                        throw e
                                    }
                                }
                            }
                        }.apply { Timber.i("Message id=${message.messageId} sent in $this") }
                        messageProcessor.onMessageDelivered()
                    } catch (e: Exception) {
                        Timber.e(e, "Error publishing message id=${message.messageId}")
                        when (e) {
                            is IOException -> messageProcessor.onMessageDeliveryFailedFinal(message.messageId)
                            is MqttException -> {
                                messageProcessor.onMessageDeliveryFailed(message.messageId)
                                reconnect(mqttConnectionConfiguration)
                                throw OutgoingMessageSendingException(e)
                            }
                            else -> {
                                messageProcessor.onMessageDeliveryFailed(message.messageId)
                                throw OutgoingMessageSendingException(e)
                            }
                        }
                    }
                }
                Timber.d("Waiting for sendmessage job for message id ${message.messageId} to finish")
                job.join()
                sendMessageThrowable?.run {
                    throw this
                }
            }
        }
    }

    override fun onPreferenceChanged(properties: List<String>) {
        if (preferences.mode != ConnectionMode.MQTT) {
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
            Preferences::tlsCaCrt.name,
            Preferences::tlsClientCrt.name,
            Preferences::tlsClientCrtPassword.name,
            Preferences::username.name,
            Preferences::ws.name
        )
        if (propertiesWeWantToReconnectOn.stream()
                .filter(properties::contains)
                .collect(Collectors.toSet())
                .isNotEmpty()
        ) {
            Timber.d("MQTT preferences changed. Reconnecting to broker")
            try {
                reconnect(getEndpointConfiguration())
            } catch (e: Exception) {
                endpointStateRepo.setState(EndpointState.ERROR_CONFIGURATION.withError(e))
            }
        }
    }

    private val mqttCallback = object : MqttCallback {
        override fun connectionLost(cause: Throwable) {
            Timber.e(cause, "Connection Lost")
            endpointStateRepo.setState(EndpointState.DISCONNECTED)
            scheduler.scheduleMqttReconnect()
        }

        override fun messageArrived(topic: String, message: MqttMessage) {
            Timber.d("Received MQTT message on $topic: ${message.id}")
            if (message.payload.isEmpty()) {
                onMessageReceived(MessageClear().apply { this.topic = topic.replace(MessageCard.BASETOPIC_SUFFIX, "") })
            }
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
                Timber.e("Enable to decrypt received message ${message.id} on $topic")
            } catch (e: JsonParseException) {
                Timber.e("Malformed JSON message received ${message.id} on $topic")
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Timber.v("Delivery complete messageId = ${token?.messageId}")
        }
    }

    /**
     * Creates an MQTT client and connects to the broker, then subscribes to the configured topics.
     */
    @kotlin.jvm.Throws(MqttException::class)
    private suspend fun connectToBroker(mqttConnectionConfiguration: MqttConnectionConfiguration) {
        withContext(ioDispatcher) {
            Timber.v("MQTT connect to Broker")
            measureTime {
                endpointStateRepo.setState(EndpointState.CONNECTING)
                try {
                    val executorService = ScheduledThreadPoolExecutor(8)
                    val mqttClient = MqttAsyncClient(
                        mqttConnectionConfiguration.connectionString,
                        mqttConnectionConfiguration.clientId,
                        MqttDefaultFilePersistence(applicationContext.noBackupFilesDir.absolutePath),
                        ScheduledExecutorPingSender(executorService),
                        executorService,
                        AndroidHighResolutionTimer()
                    ).apply {
                        Timber.i("Connecting to $mqttConnectionConfiguration")
                        connect(mqttConnectionConfiguration.getConnectOptions(applicationContext)).waitForCompletion()
                        Timber.i("Connected. Subscribing to ${mqttConnectionConfiguration.topicsToSubscribeTo}")
                        endpointStateRepo.setState(EndpointState.CONNECTED)
                        setCallback(mqttCallback)
                        subscribe(
                            mqttConnectionConfiguration.topicsToSubscribeTo.toTypedArray(),
                            IntArray(mqttConnectionConfiguration.topicsToSubscribeTo.size) {
                                mqttConnectionConfiguration.subQos.value
                            }
                        ).waitForCompletion()
                        Timber.i("Subscribed")

                        messageProcessor.notifyOutgoingMessageQueue()
                        if (preferences.publishLocationOnConnect) {
                            messageProcessor.publishLocationMessage(null) // TODO fix the trigger here
                        }
                    }
                    mqttClientAndConfiguration = MqttClientAndConfiguration(mqttClient, mqttConnectionConfiguration)
                } catch (e: Exception) {
                    when (e) {
                        is MqttSecurityException -> {
                            Timber.e("MQTT client unable to connect to endpoint: ${e.message}")
                        }
                        else -> {
                            Timber.e(e, "MQTT client unable to connect to endpoint")
                        }
                    }
                    endpointStateRepo.setState(EndpointState.ERROR.withError(e))
                }
            }.apply { Timber.d("MQTT connected in $this") }
        }
    }

    override fun reconnect(): CompletableFuture<Unit>? {
        return if (mqttClientAndConfiguration != null) {
            reconnect(mqttClientAndConfiguration!!.mqttConnectionConfiguration)
        } else {
            try {
                reconnect(getEndpointConfiguration())
            } catch (e: ConfigurationIncompleteException) {
                Timber.w(e, "MQTT Configuration not complete, cannot activate")
                endpointStateRepo.setState(EndpointState.ERROR_CONFIGURATION.withError(e))
                null
            }
        }?.asCompletableFuture()
    }

    private fun reconnect(mqttConnectionConfiguration: MqttConnectionConfiguration): Job {
        Timber.v("MQTT reconnect with configuration $mqttConnectionConfiguration")
        return scope.launch {
            connectingLock.withPermit {
                mqttConnectionIdlingResource.setIdleState(false)
                disconnect()
                connectToBroker(mqttConnectionConfiguration)
                mqttConnectionIdlingResource.setIdleState(true)
            }
        }
    }

    override fun checkConnection(): Boolean {
        return mqttClientAndConfiguration?.mqttClient?.run {
            var success = false
            checkPing(
                null,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        success = true
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        success = false
                    }
                }
            )?.apply { waitForCompletion() } ?: run { Timber.w("MQTT checkPing token was null") }
            return success
        } ?: false
    }

    class NotConnectedException : Exception()
    data class MqttClientAndConfiguration(
        val mqttClient: MqttAsyncClient,
        val mqttConnectionConfiguration: MqttConnectionConfiguration
    )
}
