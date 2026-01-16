package org.owntracks.android.net.mqtt

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.security.KeyStore
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.stream.Collectors
import javax.net.ssl.SSLHandshakeException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import kotlin.time.toDuration
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
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_EXCEPTION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECTION_LOST
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SERVER_CONNECT_ERROR
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.net.MessageProcessorEndpoint
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor
import org.owntracks.android.test.SimpleIdlingResource
import timber.log.Timber

class MQTTMessageProcessorEndpoint(
    messageProcessor: MessageProcessor,
    private val endpointStateRepo: EndpointStateRepo,
    private val scheduler: Scheduler,
    private val preferences: Preferences,
    private val parser: Parser,
    private val caKeyStore: KeyStore,
    @ApplicationScope private val scope: CoroutineScope,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val applicationContext: Context,
    private val mqttConnectionIdlingResource: SimpleIdlingResource
) :
    MessageProcessorEndpoint(messageProcessor),
    StatefulServiceMessageProcessor,
    Preferences.OnPreferenceChangeListener {

  override val modeId: ConnectionMode = ConnectionMode.MQTT

  private val connectingLock = Semaphore(1)
  private val connectivityManager: ConnectivityManager = applicationContext.getSystemService()!!
  private val alarmManager: AlarmManager = applicationContext.getSystemService()!!
  private var mqttClientAndConfiguration: MqttClientAndConfiguration? = null

  private var pingAlarmReceiver: BroadcastReceiver? = null

  private val networkChangeCallback =
      object : ConnectivityManager.NetworkCallback() {
        var justRegistered = true

        override fun onAvailable(network: Network) {
          super.onAvailable(network)
          Timber.v("Network becomes available")
          if (!justRegistered &&
              endpointStateRepo.endpointState.value == EndpointState.DISCONNECTED &&
              preferences.connectionEnabled) {
            Timber.v("Currently disconnected and connection enabled, so attempting reconnect")
            scope.launch { reconnect() }
          }
          justRegistered = false
        }

        override fun onLost(network: Network) {
          super.onLost(network)

          scope.launch { connectingLock.withPermitLogged("network lost") { disconnect() } }
        }
      }

  override fun activate() {
    Timber.v("MQTT Activate")
    preferences.registerOnPreferenceChangedListener(this)
    networkChangeCallback.justRegistered = true
    connectivityManager.registerDefaultNetworkCallback(networkChangeCallback)
    if (!preferences.connectionEnabled) {
      Timber.i("Connection is disabled by user, not connecting")
      scope.launch { endpointStateRepo.setState(EndpointState.DISCONNECTED) }
      return
    }
    scope.launch {
      try {
        val configuration = getEndpointConfiguration()
        reconnect(configuration)
      } catch (e: ConfigurationIncompleteException) {
        when (e.cause) {
          is MqttConnectionConfiguration.MissingHostException ->
              Timber.w("MQTT Configuration not complete because host is missing, cannot activate")
          else -> Timber.w(e, "MQTT Configuration not complete, cannot activate")
        }
        endpointStateRepo.setState(EndpointState.ERROR_CONFIGURATION.withError(e))
      }
    }
  }

  override fun deactivate() {
    preferences.unregisterOnPreferenceChangedListener(this)
    connectivityManager.unregisterNetworkCallback(networkChangeCallback)
    scope.launch {
      connectingLock.withPermitLogged("mqtt deactivate") { disconnect() }
      scheduler.cancelAllTasks()
    }
  }

  private val disconnectTimeout = 2.seconds

  private suspend fun disconnect() {
    withContext(ioDispatcher) {
      Timber.v("MQTT attempting Disconnect")
      measureTime {
            mqttClientAndConfiguration?.run {
              try {
                mqttClient.disconnect()
              } catch (e: MqttException) {
                when (e.reasonCode.toShort()) {
                  REASON_CODE_CLIENT_ALREADY_DISCONNECTED -> Timber.d("Client already disconnected")
                  else -> {
                    Timber.d(
                        e,
                        "Could not disconnect from client gently. Forcing disconnect with timeout=$disconnectTimeout")
                    try {
                      mqttClient.disconnectForcibly(
                          disconnectTimeout.inWholeMilliseconds,
                          disconnectTimeout.inWholeMilliseconds,
                          true)
                    } catch (e: NullPointerException) {
                      Timber.d(
                          "Could not forcibly disconnect client, NPE thrown by bug in Paho MQTT. Ignoring.")
                    }
                  }
                }
              }
              endpointStateRepo.setState(EndpointState.DISCONNECTED)
              mqttClient.close(true)
              try {
                pingAlarmReceiver?.run(applicationContext::unregisterReceiver)
                Timber.d("Unregistered ping alarm receiver")
              } catch (e: IllegalArgumentException) {
                Timber.d("Ping alarm receiver already unregistered")
              }
            }
          }
          .apply { Timber.d("MQTT disconnected in $this") }
    }
  }

  override fun getEndpointConfiguration(): MqttConnectionConfiguration {
    val configuration = preferences.toMqttConnectionConfiguration()
    configuration.validate() // Throws an exception if not valid
    return configuration
  }

  override fun onFinalizeMessage(message: MessageBase): MessageBase = message

  override suspend fun sendMessage(message: MessageBase): Result<Unit> {
    Timber.i("Sending message $message")
    if (mqttClientAndConfiguration == null) {
      return Result.failure(NotReadyException())
    }
    if (endpointStateRepo.endpointState.value != EndpointState.CONNECTED) {
      return Result.failure(NotConnectedException())
    }
    // Updates the message data + metadata with things that are in our preferences
    message.annotateFromPreferences(preferences)

    // We want to block until this completes off-thread, because we've been called sync by the
    // outgoing message loop
    return mqttClientAndConfiguration!!.run {
      runBlocking {
        var sendMessageThrowable: Throwable? = null
        val handler = CoroutineExceptionHandler { _, throwable -> sendMessageThrowable = throwable }
        val job =
            launch(ioDispatcher + CoroutineName("MQTT SendMessage") + handler) {
              try {
                Timber.d("Publishing message $message")
                measureTime {
                      while (mqttClient.inFlightMessageCount >=
                          mqttConnectionConfiguration.maxInFlight) {
                        Timber.v("Pausing to wait for inflight to drop below max")
                        delay(100.milliseconds)
                      }
                      mqttClient
                          .publish(
                              message.topic,
                              message.toJsonBytes(parser),
                              message.qos,
                              message.retained)
                          .also { Timber.v("MQTT message sent with messageId=${it.messageId}. ") }
                    }
                    .apply { Timber.i("Message ${message.messageId} sent in $this") }
              } catch (e: Exception) {
                Timber.e(e, "Error publishing message $message")
                when (e) {
                  is IOException -> messageProcessor.onMessageDeliveryFailedFinal(message)
                  is MqttException -> {
                    if (e.reasonCode.toShort() != MqttException.REASON_CODE_MAX_INFLIGHT) {
                      messageProcessor.onMessageDeliveryFailed(message)
                      Timber.w(e, "Error publishing message. Doing a reconnect")
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
        sendMessageThrowable?.run { Result.failure(this) } ?: Result.success(Unit)
      }
    }
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (preferences.mode != ConnectionMode.MQTT || mqttClientAndConfiguration == null) {
      Timber.d("Preference changed but MQTT not activated. Ignoring.")
      return
    }
    Timber.v("MQTT preferences changed: [${properties.joinToString(",")}]")
    /*
     * Similar to the HTTP endpoint, there are other preferences that trigger a complete reset, and
     * these are handled in the [MessageProcessor]. These properties below will trigger an MQTT reset
     * and attempt to redeliver the message queue head, but will not clear the queue.
     */
    val propertiesWeWantToReconnectOn =
        setOf(
            Preferences::deviceId.name,
            Preferences::keepalive.name,
            Preferences::mqttProtocolLevel.name,
            Preferences::password.name,
            Preferences::tls.name,
            Preferences::ws.name)
    if (propertiesWeWantToReconnectOn
        .stream()
        .filter(properties::contains)
        .collect(Collectors.toSet())
        .isNotEmpty()) {
      Timber.d("Reconnecting to broker because of preference change")
      scope.launch {
        try {
          reconnect(getEndpointConfiguration())
        } catch (e: Exception) {
          when (e) {
            is ConfigurationIncompleteException ->
                endpointStateRepo.setState(EndpointState.ERROR_CONFIGURATION.withError(e))
            else -> endpointStateRepo.setState(EndpointState.ERROR.withError(e))
          }
        }
      }
    }
  }

  private val mqttCallback =
      object : MqttCallbackExtended {

        override fun connectionLost(cause: Throwable) {
          when (cause) {
            is IOException -> Timber.e("Connection Lost: ${cause.message}")
            else -> Timber.e(cause, "Connection Lost")
          }
          scope.launch { endpointStateRepo.setState(EndpointState.DISCONNECTED) }
          if (preferences.connectionEnabled) {
            scheduler.scheduleMqttReconnect()
          }
        }

        override fun messageArrived(topic: String, message: MqttMessage) {
          Timber.d("Received MQTT message on $topic: message=$message")
          if (message.payload.isEmpty()) {
            onMessageReceived(
                MessageClear().apply {
                  this.topic = topic.replace(MessageCard.BASETOPIC_SUFFIX, "")
                })
          } else {
            try {
              Timber.d("Received message: ${String(message.payload)}")
              onMessageReceived(
                  parser
                      .fromJson(message.payload)
                      .apply {
                        this.topic = topic
                        this.retained = message.isRetained
                        this.qos = message.qos
                      }
                      .also { Timber.d("Parsed message: $it") })
            } catch (e: Parser.EncryptionException) {
              Timber.w("Unable to decrypt received message ${message.id} on $topic")
            } catch (e: InvalidFormatException) {
              Timber.w("Malformed JSON message received ${message.id} on $topic")
            }
          }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
          token?.run {
            Timber.v(
                "Delivery complete messageId=$messageId topics=${(topics?: emptyArray()).joinToString(",")}}")
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
  private suspend fun connectToBroker(
      mqttConnectionConfiguration: MqttConnectionConfiguration
  ): Result<Unit> =
      withContext(ioDispatcher) {
        Timber.v("MQTT connect to Broker")
        measureTimedValue {
              endpointStateRepo.setState(EndpointState.CONNECTING)
              try {
                val executorService = ScheduledThreadPoolExecutor(8)
                val pingSender =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        alarmManager.canScheduleExactAlarms()) {
                      AlarmPingSender(applicationContext)
                    } else {
                      AsyncPingSender(scope)
                    }

                MqttAsyncClient(
                        mqttConnectionConfiguration.connectionString,
                        mqttConnectionConfiguration.clientId,
                        RoomMqttClientPersistence(applicationContext),
                        pingSender,
                        executorService,
                        AndroidHighResolutionTimer())
                    .also {
                      mqttClientAndConfiguration =
                          MqttClientAndConfiguration(it, mqttConnectionConfiguration)
                    }
                    .apply {
                      mqttConnectionConfiguration
                          .getConnectOptions(applicationContext, caKeyStore)
                          .also {
                            Timber.i(
                                "Connecting to ${mqttConnectionConfiguration.connectionString} timeout = ${ it.connectionTimeout.toDuration(DurationUnit.SECONDS)}")
                          }
                          .run { connect(this).waitForCompletion() }
                      pingAlarmReceiver = MQTTPingAlarmReceiver(this)
                      ContextCompat.registerReceiver(
                              applicationContext,
                              pingAlarmReceiver,
                              IntentFilter(AlarmPingSender.PING_INTENT_ACTION),
                              ContextCompat.RECEIVER_EXPORTED)
                          .also { Timber.d("Registered ping alarm receiver") }
                      Timber.i(
                          "MQTT Connected. Subscribing to ${mqttConnectionConfiguration.topicsToSubscribeTo}")
                      endpointStateRepo.setState(EndpointState.CONNECTED)
                      setCallback(mqttCallback)
                      subscribe(
                              mqttConnectionConfiguration.topicsToSubscribeTo.toTypedArray(),
                              IntArray(mqttConnectionConfiguration.topicsToSubscribeTo.size) {
                                mqttConnectionConfiguration.subQos.value
                              })
                          .waitForCompletion()
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
                      REASON_CODE_CONNECTION_LOST.toInt() ->
                          Timber.e(
                              e.cause,
                              "MQTT client unable to connect to endpoint because the connection was lost")
                      REASON_CODE_CLIENT_EXCEPTION.toInt() ->
                          when (e.cause) {
                            is SSLHandshakeException ->
                                Timber.e("$errorLog: ${(e.cause as SSLHandshakeException).message}")
                            is UnknownHostException ->
                                Timber.e(
                                    "$errorLog: Unknown host \"${(e.cause as UnknownHostException).message}\"")
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
                if (preferences.connectionEnabled) {
                  scheduler.scheduleMqttReconnect()
                }
                Result.failure(e)
              }
            }
            .apply { Timber.d("MQTT connection attempt completed in ${this.duration}") }
            .value
      }

  override suspend fun reconnect(): Result<Unit> =
      mqttClientAndConfiguration?.mqttConnectionConfiguration?.run { reconnect(this) }
          ?: run { reconnect(getEndpointConfiguration()) }

  private suspend fun reconnect(
      mqttConnectionConfiguration: MqttConnectionConfiguration
  ): Result<Unit> =
      connectingLock.withPermitLogged("MQTT reconnect") {
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
      val token =
          checkPing(
              null,
              object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                  success = true
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                  success = false
                }
              })
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
}

/**
 * Wrapper around [Semaphore.withPermit] that logs when the lock is acquired and released
 *
 * @param label
 * @param function
 * @return
 * @receiver
 */
private suspend fun <T> Semaphore.withPermitLogged(label: String, function: suspend () -> T): T {
  return also {
        Timber.v("Waiting for lock. label=$label available permits = ${it.availablePermits}")
      }
      .withPermit {
        Timber.v("lock acquired label=$label")
        try {
          function()
        } catch (e: Exception) {
          Timber.e(e, "BOO")
          throw e
        } finally {
          Timber.v("lock released label=$label")
        }
      }
}
