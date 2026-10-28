package org.owntracks.android.net.mqtt

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.security.KeyStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.stream.Collectors
import javax.net.ssl.SSLHandshakeException
import kotlin.time.Duration
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_EXCEPTION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECTION_LOST
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SERVER_CONNECT_ERROR
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.EndpointStatus
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
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val applicationContext: Context,
    private val mqttConnectionIdlingResource: SimpleIdlingResource,
) :
    MessageProcessorEndpoint(messageProcessor),
    StatefulServiceMessageProcessor,
    Preferences.OnPreferenceChangeListener {

  override val modeId: ConnectionMode = ConnectionMode.MQTT

  private val connectingLock = Semaphore(1)
  private val connectivityManager: ConnectivityManager = applicationContext.getSystemService()!!
  private val alarmManager: AlarmManager = applicationContext.getSystemService()!!
  @Volatile
  @VisibleForTesting
  internal var mqttClientAndConfiguration: MqttClientAndConfiguration? = null

  private var pingAlarmReceiver: BroadcastReceiver? = null

  internal val networkChangeCallback =
      NetworkTrackingCallback(
          { endpointStateRepo.endpointState.value.state },
          { scope.launch { reconnect() } },
          {
            scope.launch {
              connectingLock.withPermitLogged("network lost") { disconnect() }
              // Schedule a reconnect in case onAvailable already ran and its reconnect completed
              // before this coroutine acquired the lock — without this, the freshly-established
              // connection is torn down and no further reconnect is ever triggered.
              scheduler.scheduleMqttReconnect()
            }
          },
      )

  override fun activate() {
    Timber.v("MQTT Activate")
    preferences.registerOnPreferenceChangedListener(this)
    networkChangeCallback.reset()
    connectivityManager.registerDefaultNetworkCallback(networkChangeCallback)
    // Backstop for everything the connectivity callbacks and the reconnect worker between them
    // fail to notice — most importantly a connection that is dead but still believed to be up.
    scheduler.scheduleMqttConnectionWatchdog()
    scope.launch {
      try {
        val configuration = getEndpointConfiguration()
        reconnect(configuration)
      } catch (e: ConfigurationIncompleteException) {
        when (e.cause) {
          is MqttConnectionConfiguration.MissingHostException ->
              Timber.e("MQTT Configuration not complete because host is missing, cannot activate")
          else -> Timber.e(e, "MQTT Configuration not complete, cannot activate")
        }
        setEndpointState(EndpointState.ERROR_CONFIGURATION.withError(e))
      }
    }
  }

  /*
  Set when this endpoint is replaced. It may still be winding down (an MQTT disconnect finishes
  asynchronously) or get late callbacks, and none of that should overwrite the state of the endpoint
  that replaced it.
   */
  @Volatile private var deactivated = false

  private suspend fun setEndpointState(status: EndpointStatus) {
    if (deactivated) {
      Timber.v("Ignoring endpoint state $status from a deactivated endpoint")
      return
    }
    endpointStateRepo.setState(status)
  }

  private suspend fun setEndpointState(state: EndpointState) =
      setEndpointState(EndpointStatus(state))

  override fun deactivate() {
    deactivated = true
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
          // A client is torn down once. disconnect() closes it but keeps the reference
          // (onPreferenceChanged reads null as "MQTT not active"), so a "network lost" disconnect
          // followed by reconnect()'s own disconnect used to reach the closed client a second time,
          // and Paho's disconnectForcibly NPEs on a closed client's nulled internals.
          if (closed) {
            Timber.v("MQTT client already torn down")
            return@run
          }
          closed = true
          try {
            // A client that isn't connected (its CONNACK wait timed out, or Paho already shut it
            // down after losing the connection) is torn down forcibly, which is synchronous.
            // disconnect() would hand the teardown to Paho's DisconnectBG, which first waits up to
            // 30 s for outstanding tokens (the stuck connect's included); close() right after it
            // only marks the client close-pending, and with a close pending Paho's own shutdown
            // returns early, so the client's socket and WebSocket receiver were never released.
            // No DISCONNECT is sent over a link that isn't usable: the broker sees the connection
            // drop, and publishes the will if one is set.
            if (!mqttClient.isConnected) {
              mqttClient.disconnectForcibly(0, disconnectTimeout.inWholeMilliseconds, false)
            } else {
              mqttClient.disconnect()
            }
          } catch (e: MqttException) {
            when (e.reasonCode.toShort()) {
              REASON_CODE_CLIENT_ALREADY_DISCONNECTED -> Timber.d("Client already disconnected")
              else -> {
                Timber.d(
                    e,
                    "Could not disconnect from client gently. Forcing disconnect with timeout=$disconnectTimeout",
                )
                try {
                  mqttClient.disconnectForcibly(
                      disconnectTimeout.inWholeMilliseconds,
                      disconnectTimeout.inWholeMilliseconds,
                      true,
                  )
                } catch (e: Exception) {
                  Timber.d(e, "Could not forcibly disconnect MQTT client. Ignoring.")
                }
              }
            }
          } catch (e: RuntimeException) {
            // An unchecked exception from inside Paho must not kill the process; the client is
            // closed below either way.
            Timber.w(e, "Paho failed to disconnect the MQTT client. Closing it anyway.")
          }
          setEndpointState(EndpointState.DISCONNECTED)
          try {
            mqttClient.close(true)
          } catch (e: Exception) {
            Timber.w(e, "Error closing MQTT client. Ignoring.")
          }
          // Paho never shuts down an executor it was handed (1.2.5 defines
          // ClientComms.shutdownExecutorService but nothing calls it), so each connect's pool, and
          // its idle core threads, outlived the client. shutdown(), not shutdownNow(): Paho's own
          // async disconnect may still be queued on this pool, and dropping it would skip the
          // DISCONNECT (the broker would then publish the will).
          executorService?.shutdown()
          try {
            pingAlarmReceiver?.run(applicationContext::unregisterReceiver)
            Timber.d("Unregistered ping alarm receiver")
          } catch (_: IllegalArgumentException) {
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
    Timber.d("Sending message $message")
    val clientAndConfiguration =
        mqttClientAndConfiguration ?: return Result.failure(NotReadyException())
    if (endpointStateRepo.endpointState.value.state != EndpointState.CONNECTED) {
      return Result.failure(NotConnectedException())
    }
    // Updates the message data + metadata with things that are in our preferences
    message.annotateFromPreferences(preferences)

    if (!awaitInFlightCapacity(clientAndConfiguration)) {
      return Result.failure(NotConnectedException())
    }

    // We want to block until this completes off-thread, because we've been called sync by the
    // outgoing message loop
    return clientAndConfiguration.run {
      runBlocking {
        var sendMessageThrowable: Throwable? = null
        val handler = CoroutineExceptionHandler { _, throwable -> sendMessageThrowable = throwable }
        val job =
            launch(ioDispatcher + CoroutineName("MQTT SendMessage") + handler) {
              try {
                Timber.d("Publishing message $message")
                measureTime {
                  mqttClient
                      .publish(
                          message.topic,
                          message.toJsonBytes(parser),
                          message.qos,
                          message.retained,
                      )
                      .also { Timber.v("MQTT message sent with messageId=${it.messageId}. ") }
                }
                    .apply { Timber.i("Message $message sent in $this") }
              } catch (e: Exception) {
                Timber.w(e, "Error publishing message $message")
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

  /**
   * Waits for the client's in-flight message count to drop below the configured max.
   *
   * Bounded, and gives up as soon as [clientAndConfiguration] stops being the current, connected
   * client: a client that's been replaced by a reconnect never gets its in-flight messages acked,
   * so waiting on it would wedge the outbound message loop forever while the new client sits idle.
   *
   * @return true if there's capacity to publish on this client, false if the message should be
   *   re-queued
   */
  private suspend fun awaitInFlightCapacity(
      clientAndConfiguration: MqttClientAndConfiguration
  ): Boolean {
    val (client, configuration) = clientAndConfiguration
    // A closed client has no capacity. Reading its in-flight count NPEs (Paho nulls its state on
    // close), and MessageProcessor drops a message whose send throws; returning false re-queues it.
    if (clientAndConfiguration.closed) return false
    if (client.inFlightMessageCount < configuration.maxInFlight) {
      return true
    }
    Timber.d(
        "MQTT in-flight count ${client.inFlightMessageCount} is at max " +
            "${configuration.maxInFlight}, waiting for it to drop"
    )
    return withTimeoutOrNull(IN_FLIGHT_WAIT_TIMEOUT) {
      while (
          clientAndConfiguration.closed || client.inFlightMessageCount >= configuration.maxInFlight
      ) {
        if (
            clientAndConfiguration.closed ||
                mqttClientAndConfiguration !== clientAndConfiguration ||
                endpointStateRepo.endpointState.value.state != EndpointState.CONNECTED
        ) {
          Timber.w("MQTT client replaced or disconnected while waiting for in-flight to drop")
          return@withTimeoutOrNull false
        }
        delay(100.milliseconds)
      }
      true
    }
        ?: false.also {
          Timber.w(
              "Timed out after $IN_FLIGHT_WAIT_TIMEOUT waiting for MQTT in-flight count " +
                  "${client.inFlightMessageCount} to drop below ${configuration.maxInFlight}"
          )
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
            Preferences::ws.name,
        )
    if (
        propertiesWeWantToReconnectOn
            .stream()
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
            is ConfigurationIncompleteException ->
                setEndpointState(EndpointState.ERROR_CONFIGURATION.withError(e))
            else -> setEndpointState(EndpointState.ERROR.withError(e))
          }
        }
      }
    }
  }

  private val mqttCallback =
      object : MqttCallbackExtended {

        override fun connectionLost(cause: Throwable) {
          when (cause) {
            is IOException -> Timber.w("Connection Lost: ${cause.message}")
            else -> Timber.w(cause, "Connection Lost")
          }
          scope.launch { setEndpointState(EndpointState.DISCONNECTED) }
          scheduler.scheduleMqttReconnect()
        }

        override fun messageArrived(topic: String, message: MqttMessage) {
          Timber.d("Received MQTT message on $topic: message=$message")
          if (message.payload.isEmpty()) {
            onMessageReceived(
                MessageClear().apply {
                  this.topic = topic.replace(MessageCard.BASETOPIC_SUFFIX, "")
                }
            )
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
                      .also { Timber.d("Parsed message: $it") }
              )
            } catch (_: Parser.EncryptionException) {
              Timber.e("Unable to decrypt received message ${message.id} on $topic")
            } catch (_: SerializationException) {
              Timber.w("Malformed JSON message received ${message.id} on $topic")
            }
          }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
          token?.run {
            Timber.v(
                "Delivery complete messageId=$messageId topics=${(topics?: emptyArray()).joinToString(",")}}"
            )
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
          setEndpointState(EndpointState.CONNECTING)
          try {
            val executorService = ScheduledThreadPoolExecutor(8)
            val pingSender =
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        alarmManager.canScheduleExactAlarms()
                ) {
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
                    AndroidHighResolutionTimer(),
                )
                .also {
                  mqttClientAndConfiguration =
                      MqttClientAndConfiguration(it, mqttConnectionConfiguration, executorService)
                }
                .apply {
                  // Every wait on a broker reply is bounded. Paho has no timeout of its own for the
                  // CONNACK or SUBACK, and this runs under connectingLock, so one unanswered
                  // connect used to park every later reconnect (the watchdog's included) behind
                  // the lock for good.
                  val replyWait = completionWait(mqttConnectionConfiguration.timeout)
                  mqttConnectionConfiguration
                      .getConnectOptions(applicationContext, caKeyStore)
                      .also {
                        Timber.d(
                            "Connecting to ${mqttConnectionConfiguration.connectionString} timeout = ${ it.connectionTimeout.toDuration(DurationUnit.SECONDS)}"
                        )
                      }
                      .run { connect(this).waitForCompletion(replyWait.inWholeMilliseconds) }
                  pingAlarmReceiver = MQTTPingAlarmReceiver(this)
                  ContextCompat.registerReceiver(
                          applicationContext,
                          pingAlarmReceiver,
                          IntentFilter(AlarmPingSender.PING_INTENT_ACTION),
                          ContextCompat.RECEIVER_EXPORTED,
                      )
                      .also { Timber.d("Registered ping alarm receiver") }
                  Timber.i(
                      "MQTT Connected. Subscribing to ${mqttConnectionConfiguration.topicsToSubscribeTo}"
                  )
                  setEndpointState(EndpointState.CONNECTED)
                  // This run of failures is over, so the next one starts from the short delay
                  // again rather than inheriting however far this one had backed off.
                  scheduler.resetMqttReconnectBackoff()
                  setCallback(mqttCallback)
                  subscribe(
                          mqttConnectionConfiguration.topicsToSubscribeTo.toTypedArray(),
                          IntArray(mqttConnectionConfiguration.topicsToSubscribeTo.size) {
                            mqttConnectionConfiguration.subQos.value
                          },
                      )
                      .waitForCompletion(replyWait.inWholeMilliseconds)
                  Timber.d("MQTT Subscribed")

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
                  REASON_CODE_CONNECTION_LOST.toInt() -> {
                    Timber.d(e.cause)
                    Timber.w(
                        "MQTT client unable to connect to endpoint because the connection was lost",
                    )
                  }
                  REASON_CODE_CLIENT_EXCEPTION.toInt() ->
                      when (e.cause) {
                        is SSLHandshakeException ->
                            Timber.e("$errorLog: ${(e.cause as SSLHandshakeException).message}")
                        is UnknownHostException ->
                            Timber.e(
                                "$errorLog: Unknown host \"${(e.cause as UnknownHostException).message}\""
                            )
                        else -> Timber.e(e, errorLog)
                      }
                  REASON_CODE_SERVER_CONNECT_ERROR.toInt() -> {
                    when (e.cause) {
                      is ConnectException -> {
                        Timber.w("$errorLog: ${(e.cause as ConnectException).message}")
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
            setEndpointState(EndpointState.ERROR.withError(e))
            scheduler.scheduleMqttReconnect()
            Result.failure(e)
          }
        }
            .apply { Timber.d("MQTT connection attempt completed in ${this.duration}") }
            .value
      }

  override suspend fun reconnect(): Result<Unit> =
      mqttClientAndConfiguration?.mqttConnectionConfiguration?.run { reconnect(this) }
          ?: try {
            reconnect(getEndpointConfiguration())
          } catch (e: ConfigurationIncompleteException) {
            Timber.w("MQTT not configured, skipping reconnect: ${e.message}")
            setEndpointState(EndpointState.ERROR_CONFIGURATION.withError(e))
            Result.failure(e)
          }

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

  /**
   * Establishes whether the connection is actually usable, rather than merely believed to be. A TCP
   * connection that has gone away without a FIN — routine when a mobile network drops or a NAT
   * entry expires — stays readable as "connected" until something is written to it, so the endpoint
   * state on its own is not evidence of a working link.
   *
   * Paho's `checkPing` sends a PINGREQ (and returns its token) only when a keepalive ping is due;
   * with traffic inside the keepalive interval it returns null. Reading that null as "dead" made
   * the watchdog tear down a healthy connection on nearly every run. So: a due ping must come back;
   * with none due, the link is healthy if Paho believes it is connected and nothing it sent is
   * still awaiting the broker's acknowledgement (a dead link with traffic keeps messages in
   * flight).
   *
   * Blocking, and called from the connection watchdog off the main thread.
   */
  override fun checkConnection(): Boolean =
      mqttClientAndConfiguration?.mqttClient?.let { client ->
        try {
          val token = client.checkPing(null, null)
          if (token == null) {
            // No ping due. Healthy if connected and what was sent gets acknowledged: a publish may
            // be in flight right now (a ping batched with this run, a backlog draining), so give
            // acknowledgements [inFlightGrace] to arrive.
            client.isConnected && inFlightDrains(client)
          } else {
            // Completing without an exception means the PINGRESP arrived.
            token.waitForCompletion(PING_CHECK_TIMEOUT.inWholeMilliseconds)
            true
          }
        } catch (e: Exception) {
          // A timeout waiting for the PINGRESP arrives here (a keepalive failure inside checkPing
          // is handled by Paho, which shuts the connection down and returns null). Either way the
          // connection is not usable.
          Timber.w(e, "MQTT connection check failed")
          false
        }
      } ?: false

  /** How long [checkConnection] waits for in-flight messages to be acknowledged. */
  @VisibleForTesting internal var inFlightGrace: Duration = PING_CHECK_TIMEOUT

  /**
   * Whether the in-flight count drops to zero within [inFlightGrace]. `<= 0` counts as drained:
   * Paho's count can go negative after a resumed QoS 2 flow.
   */
  private fun inFlightDrains(client: MqttAsyncClient): Boolean {
    val deadline = System.nanoTime() + inFlightGrace.inWholeNanoseconds
    while (client.inFlightMessageCount > 0) {
      if (System.nanoTime() >= deadline) return false
      Thread.sleep(IN_FLIGHT_POLL.inWholeMilliseconds)
    }
    return true
  }

  class NotConnectedException : Exception()

  companion object {
    /** Bounded so the watchdog cannot be parked indefinitely on an unresponsive broker. */
    private val PING_CHECK_TIMEOUT = 10.seconds

    /** How often [inFlightDrains] re-reads the in-flight count. */
    private val IN_FLIGHT_POLL = 250.milliseconds

    /** Bounded so the outbound message loop cannot be parked indefinitely on a stuck client. */
    private val IN_FLIGHT_WAIT_TIMEOUT = 30.seconds

    /**
     * Slack on top of the configured connection timeout before giving up on a broker reply. Paho
     * applies that timeout to the socket connect and the TLS handshake, not to the CONNACK, so the
     * whole wait gets a margin rather than the bare value.
     */
    private val COMPLETION_WAIT_MARGIN = 30.seconds

    /**
     * The longest a connect or subscribe may wait for the broker's reply. Never zero: Paho reads a
     * wait of 0 as "forever".
     */
    internal fun completionWait(connectionTimeout: Duration): Duration =
        connectionTimeout.coerceAtLeast(1.seconds) + COMPLETION_WAIT_MARGIN
  }

  data class MqttClientAndConfiguration(
      val mqttClient: MqttAsyncClient,
      val mqttConnectionConfiguration: MqttConnectionConfiguration,
      /** The pool this client runs on, shut down with it in [disconnect]. */
      val executorService: ExecutorService? = null,
  ) {
    /** Set once [disconnect] has torn this client down (closed it). Never touch it again. */
    @Volatile var closed: Boolean = false
  }
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
          throw e
        } finally {
          Timber.v("lock released label=$label")
        }
      }
}
