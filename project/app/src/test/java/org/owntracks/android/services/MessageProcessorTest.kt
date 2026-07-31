package org.owntracks.android.services

import android.content.Context
import dagger.Lazy
import java.io.File
import java.security.KeyStore
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.data.repos.AsyncDeQueue
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.test.IdlingResourceWithData
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface

/**
 * Tests for the lifecycle of [MessageProcessor]'s outbound sender loop.
 *
 * [MessageProcessor] is a `@Singleton`, but the `BackgroundService` that drives it is not: the
 * service can be destroyed and recreated while the process survives. `onDestroy` cancels the sender
 * loop via [MessageProcessor.stopSendingMessages], so every subsequent service start has to re-arm
 * it. Failing to do so is what left users with a queue growing for hours and nothing ever published
 * until the app was force-stopped (see issue #2294).
 *
 * The loop itself is private, so these tests observe it through the queue: a live loop parks in
 * [AsyncDeQueue.awaitMessage] waiting for work, so reaching that call is the signal that the loop is
 * running.
 */
class MessageProcessorTest {

  /**
   * A queue that never yields a message, so a running loop parks in [awaitMessage] exactly as it
   * does against a real empty queue, and reports when it gets there.
   */
  private class LoopDetectingQueue : AsyncDeQueue {
    override val queueSize: StateFlow<Int> = MutableStateFlow(0)

    /** Never receives anything; parks the caller until it is cancelled. */
    private val neverAnyMessages = Channel<MessageBase>()

    @Volatile private var loopReachedAwait: CompletableDeferred<Unit>? = null

    /**
     * Arms a fresh detector. Awaiting the returned deferred completes once a sender loop reaches
     * [awaitMessage]; it timing out means no loop is running.
     */
    fun armLoopDetector(): CompletableDeferred<Unit> =
        CompletableDeferred<Unit>().also { loopReachedAwait = it }

    override suspend fun awaitMessage(): MessageBase {
      loopReachedAwait?.complete(Unit)
      return neverAnyMessages.receive()
    }

    override suspend fun initialize(legacyStoragePath: File) {}

    override suspend fun enqueue(message: MessageBase): Boolean = true

    override suspend fun requeue(message: MessageBase): Boolean = true

    override suspend fun dequeue(): MessageBase? = null

    override fun signalMessageAvailable() {}

    override suspend fun size(): Int = 0

    override suspend fun clear() {}

    override fun close() {}
  }

  /*
  A real multi-threaded dispatcher rather than an unconfined test one. Under UnconfinedTestDispatcher
  the nested `scope.launch` inside loadOutgoingMessageProcessor runs on the enclosing coroutine's
  unconfined event loop, so a failure there propagates into its caller instead of going to the
  scope's handler — which is not how these independently-dispatched jobs behave in the app. The
  tests here are about liveness rather than ordering, and each one waits on an outcome with a
  timeout, so real dispatch costs nothing in determinism.
   */
  private val testDispatcher = Dispatchers.Default

  // SupervisorJob + a swallowing handler mirrors the real application scope. Activating an endpoint
  // against mocked Preferences throws, which is deliberate: the sender loop is supposed to run
  // regardless of whether the endpoint came up, and these tests assert exactly that.
  private val testScope =
      CoroutineScope(SupervisorJob() + testDispatcher + CoroutineExceptionHandler { _, _ -> })

  private lateinit var queue: LoopDetectingQueue
  private lateinit var messageProcessor: MessageProcessor

  @Before
  fun setUp() {
    queue = LoopDetectingQueue()
    messageProcessor =
        MessageProcessor(
            applicationContext = mock<Context> { on { filesDir } doReturn File("/tmp") },
            contactsRepo = mock {},
            preferences = mock { on { mode } doReturn ConnectionMode.HTTP },
            waypointsRepo = mock {},
            parser = mock<Parser> {},
            scheduler = mock<Scheduler> {},
            endpointStateRepo = EndpointStateRepo(),
            outgoingQueueIdlingResource =
                mock<ThresholdIdlingResourceInterface> { on { isIdleNow } doReturn true },
            importConfigurationIdlingResource = SimpleIdlingResource("import", true),
            messageReceivedIdlingResource = mock<IdlingResourceWithData<MessageBase>> {},
            caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also { it.load(null) },
            locationProcessorLazy = mock<Lazy<LocationProcessor>> {},
            ioDispatcher = testDispatcher,
            scope = testScope,
            mqttConnectionIdlingResource = SimpleIdlingResource("mqtt", true),
            outgoingQueue = queue)
  }

  /** Fails the test if no sender loop reaches [AsyncDeQueue.awaitMessage] in time. */
  private fun assertLoopRunning(detector: CompletableDeferred<Unit>, message: String) = runBlocking {
    assertNotNull(message, withTimeoutOrNull(TIMEOUT) { detector.await() })
  }

  @Test
  fun `initialize starts the outbound sender loop`() {
    val detector = queue.armLoopDetector()

    messageProcessor.initialize()

    assertLoopRunning(detector, "sender loop should be running after initialize")
  }

  @Test
  fun `stopSendingMessages stops the outbound sender loop`() {
    messageProcessor.initialize()
    assertLoopRunning(queue.armLoopDetector(), "sender loop should be running after initialize")

    messageProcessor.stopSendingMessages()

    // A fresh detector must not fire: nothing should be re-entering awaitMessage any more.
    val detector = queue.armLoopDetector()
    runBlocking {
      assertNull(
          "sender loop should be stopped", withTimeoutOrNull(SETTLE) { detector.await() })
    }
  }

  /**
   * The regression test for issue #2294. Before the fix, `initialized` was a one-shot flag that was
   * never reset, so the second `initialize()` was a complete no-op and the loop stayed dead for the
   * remaining life of the process.
   */
  @Test
  fun `initialize re-arms the sender loop after the background service is destroyed and recreated`() {
    messageProcessor.initialize() // service starts
    assertLoopRunning(queue.armLoopDetector(), "sender loop should be running after initialize")

    messageProcessor.stopSendingMessages() // BackgroundService.onDestroy

    val detector = queue.armLoopDetector()
    messageProcessor.initialize() // service is recreated within the surviving process

    assertLoopRunning(detector, "sender loop should have been re-armed by the second initialize")
  }

  /** Whatever triggered the reconnect, the queue still needs something alive to drain it. */
  @Test
  fun `reconnect re-arms the sender loop`() {
    messageProcessor.initialize()
    assertLoopRunning(queue.armLoopDetector(), "sender loop should be running after initialize")

    messageProcessor.stopSendingMessages()

    val detector = queue.armLoopDetector()
    runBlocking { messageProcessor.reconnect() }

    assertLoopRunning(detector, "sender loop should have been re-armed by reconnect")
  }

  /**
   * The loop guards itself with a mutex so that concurrent starts cannot produce two loops racing
   * for the same queue. Repeated starts must therefore be harmless no-ops rather than duplicates.
   */
  @Test
  fun `repeated initialize calls do not start a second loop`() {
    messageProcessor.initialize()
    assertLoopRunning(queue.armLoopDetector(), "sender loop should be running after initialize")

    // Already-running loop is parked in awaitMessage; a second loop would have to call it again.
    val detector = queue.armLoopDetector()
    messageProcessor.initialize()
    messageProcessor.initialize()

    runBlocking {
      assertNull(
          "a second sender loop should not have been started",
          withTimeoutOrNull(SETTLE) { detector.await() })
    }
  }

  /**
   * The watchdog asks this on every run, including in HTTP mode where there is no persistent
   * connection to check. Reporting unhealthy there would have it reconnect an endpoint that was
   * never connected in the first place, every fifteen minutes, forever.
   */
  @Test
  fun `checkConnection reports healthy for an endpoint with no persistent connection`() {
    messageProcessor.initialize() // preferences.mode is HTTP
    assertLoopRunning(queue.armLoopDetector(), "sender loop should be running after initialize")

    runBlocking { assertTrue(messageProcessor.checkConnection()) }
  }

  companion object {
    /** Generous: only reached when a test is about to fail anyway. */
    private val TIMEOUT = 10.seconds
    /** Time given to a loop that should *not* start, to prove it doesn't. */
    private val SETTLE = 500.milliseconds
  }
}
