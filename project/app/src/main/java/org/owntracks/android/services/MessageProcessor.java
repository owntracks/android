package org.owntracks.android.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.data.EndpointState;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.EndpointStateRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.di.IoDispatcher;
import org.owntracks.android.model.messages.MessageBase;
import org.owntracks.android.model.messages.MessageCard;
import org.owntracks.android.model.messages.MessageClear;
import org.owntracks.android.model.messages.MessageCmd;
import org.owntracks.android.model.messages.MessageLocation;
import org.owntracks.android.model.messages.MessageTransition;
import org.owntracks.android.preferences.Preferences;
import org.owntracks.android.preferences.types.ConnectionMode;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.ServiceBridge;
import org.owntracks.android.support.SimpleIdlingResource;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineDispatcher;
import timber.log.Timber;

@Singleton
public class MessageProcessor implements Preferences.OnPreferenceChangeListener {
    private final EventBus eventBus;
    private final ContactsRepo contactsRepo;
    private final WaypointsRepo waypointsRepo;
    private final Context applicationContext;
    private final Preferences preferences;
    private final Parser parser;
    private final Scheduler scheduler;
    private final Lazy<LocationProcessor> locationProcessorLazy;

    private final EndpointStateRepo endpointStateRepo;
    private final ServiceBridge serviceBridge;
    private final CountingIdlingResource outgoingQueueIdlingResource;
    private final RunThingsOnOtherThreads runThingsOnOtherThreads;
    private final CoroutineDispatcher ioDispatcher;
    private MessageProcessorEndpoint endpoint;

    private boolean acceptMessages = false;
    private final BlockingDeque<MessageBase> outgoingQueue;
    private Thread backgroundDequeueThread;

    private static final long SEND_FAILURE_BACKOFF_INITIAL_WAIT = TimeUnit.SECONDS.toMillis(1);
    private static final long SEND_FAILURE_BACKOFF_MAX_WAIT = TimeUnit.MINUTES.toMillis(2);

    private boolean initialized = false;

    private final Object locker = new Object();
    private ScheduledFuture<?> waitFuture = null;
    private long retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;

    private final MessageProcessorEndpoint httpEndpoint;
    private final MessageProcessorEndpoint mqttEndpoint;

    @Inject
    public MessageProcessor(
            @ApplicationContext Context applicationContext,
            EventBus eventBus,
            ContactsRepo contactsRepo,
            Preferences preferences,
            WaypointsRepo waypointsRepo,
            Parser parser,
            Scheduler scheduler,
            EndpointStateRepo endpointStateRepo,
            ServiceBridge serviceBridge,
            RunThingsOnOtherThreads runThingsOnOtherThreads,
            CountingIdlingResource outgoingQueueIdlingResource,
            Lazy<LocationProcessor> locationProcessorLazy,
            @IoDispatcher CoroutineDispatcher ioDispatcher
    ) {
        this.applicationContext = applicationContext;
        this.preferences = preferences;
        this.eventBus = eventBus;
        this.contactsRepo = contactsRepo;
        this.waypointsRepo = waypointsRepo;
        this.parser = parser;
        this.scheduler = scheduler;
        this.locationProcessorLazy = locationProcessorLazy;
        this.endpointStateRepo = endpointStateRepo;
        this.serviceBridge = serviceBridge;
        this.outgoingQueueIdlingResource = outgoingQueueIdlingResource;
        this.eventBus.register(this);
        this.runThingsOnOtherThreads = runThingsOnOtherThreads;
        this.ioDispatcher = ioDispatcher;

        outgoingQueue = new BlockingDequeThatAlsoSometimesPersistsThingsToDiskMaybe(10000, applicationContext.getFilesDir(), parser);
        synchronized (outgoingQueue) {
            for (int i = 0; i < outgoingQueue.size(); i++) {
                outgoingQueueIdlingResource.increment();
            }
            Timber.d("Initializing the outgoingqueueidlingresource at %s", outgoingQueue.size());
        }
        preferences.registerOnPreferenceChangedListener(this);
        httpEndpoint = new MessageProcessorEndpointHttp(this, this.parser, this.preferences, this.scheduler, this.applicationContext, this.endpointStateRepo);
        mqttEndpoint = new MQTTMessageProcessorEndpoint(this, this.endpointStateRepo, this.scheduler, this.preferences, this.parser, this.ioDispatcher, applicationContext);
    }

    synchronized public void initialize() {
        if (!initialized) {
            Timber.d("Initializing MessageProcessor");
            endpointStateRepo.setState(EndpointState.INITIAL);
            reconnect();
            initialized = true;
        }
    }

    /**
     * Called either by the connection activity user button, or by receiving a RECONNECT message
     */
    public CompletableFuture<Unit> reconnect() {
        if (endpoint == null) {
            loadOutgoingMessageProcessor(); // The processor should take care of the reconnect on init
            return null;
        } else if (endpoint instanceof MQTTMessageProcessorEndpoint) {
            return ((MQTTMessageProcessorEndpoint) endpoint).reconnect();
        } else {
            return null;
        }
    }

    public boolean statefulCheckConnection() {
        if (endpoint instanceof StatefulServiceMessageProcessor)
            return ((StatefulServiceMessageProcessor) endpoint).checkConnection();
        else
            return true;
    }

    public boolean isEndpointReady() {
        try {
            if (this.endpoint != null) {
                this.endpoint.getEndpointConfiguration();
                return true;
            }
        } catch (ConfigurationIncompleteException e) {
            return false;
        }
        return false;
    }

    private void loadOutgoingMessageProcessor() {
        Timber.d("Reloading outgoing message processor. ThreadID: %s", Thread.currentThread());
        if (endpoint != null) {
            Timber.d("Destroying previous endpoint");
            endpoint.deactivate();
        }

        endpointStateRepo.setQueueLength(outgoingQueue.size());

        switch (preferences.getMode()) {
            case HTTP:
                this.endpoint = httpEndpoint;
                break;
            case MQTT:
            default:
                this.endpoint = mqttEndpoint;

        }

        if (backgroundDequeueThread == null || !backgroundDequeueThread.isAlive()) {
            // Create the background thread that will handle outbound msgs
            backgroundDequeueThread = new Thread(this::sendAvailableMessages, "backgroundDequeueThread");
            backgroundDequeueThread.start();
        }

        this.endpoint.activate();
        acceptMessages = true;
    }

    public void queueMessageForSending(MessageBase message) {
        if (!acceptMessages) return;
        outgoingQueueIdlingResource.increment();
        Timber.d("Queueing messageId:%s, current queueLength:%s", message.getMessageId(), outgoingQueue.size());
        synchronized (outgoingQueue) {
            if (!outgoingQueue.offer(message)) {
                MessageBase droppedMessage = outgoingQueue.poll();
                Timber.e("Outgoing queue full. Dropping oldest message: %s", droppedMessage);
                if (!outgoingQueue.offer(message)) {
                    Timber.e("Still can't put message onto the queue. Dropping: %s", message);
                }
            }
            endpointStateRepo.setQueueLength(outgoingQueue.size());
        }
    }

    // Should be on the background thread here, because we block
    private void sendAvailableMessages() {
        Timber.d("Starting outbound message loop. ThreadID: %s", Thread.currentThread());
        boolean previousMessageFailed = false;
        int retriesToGo = 0;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        while (true) {
            try {
                MessageBase message;
                message = this.outgoingQueue.take(); // <--- blocks
                Timber.v("Taken message off queue: %s", message);
                endpointStateRepo.setQueueLength(outgoingQueue.size() + 1);
                if (!previousMessageFailed) {
                    retriesToGo = message.getNumberOfRetries();
                }

                /*
                We need to run the actual network sending part on a different thread because the
                implementation might not be thread-safe. So we wrap `sendMessage()` up in a callable
                and a FutureTask and then dispatch it off to the network thread, and block on the
                return, handling any exceptions that might have been thrown.
                */
                Callable<Void> sendMessageCallable = () -> {
                    this.endpoint.sendMessage(message);
                    return null;
                };
                FutureTask<Void> futureTask = new FutureTask<>(sendMessageCallable);
                runThingsOnOtherThreads.postOnNetworkHandlerDelayed(futureTask, 1);
                try {
                    try {
                        futureTask.get();
                    } catch (ExecutionException e) {
                        if (e.getCause() != null) {
                            throw e.getCause();
                        } else {
                            throw new Exception("sendMessage failed, but no exception actually given");
                        }
                    }
                    previousMessageFailed = false;
                    retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
                } catch (OutgoingMessageSendingException | ConfigurationIncompleteException e) {
                    Timber.w("Error sending message. Re-queueing");
                    // Let's do a little dance to hammer this failed message back onto the head of the
                    // queue. If someone's queued something on the tail in the meantime and the queue
                    // is now empty, then throw that latest message away.
                    synchronized (this.outgoingQueue) {
                        if (!this.outgoingQueue.offerFirst(message)) {
                            MessageBase tailMessage = this.outgoingQueue.removeLast();
                            Timber.w("Queue full when trying to re-queue failed message. Dropping last message: %s", tailMessage);
                            if (!this.outgoingQueue.offerFirst(message)) {
                                Timber.e("Couldn't restore failed message back onto the head of the queue, dropping: %s", message);
                            }
                        }
                    }
                    previousMessageFailed = true;
                    retriesToGo -= 1;
                } catch (IOException e) {
                    retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
                    previousMessageFailed = false;
                    // Deserialization failure, drop and move on
                } catch (Throwable e) {
                    Timber.e(e, "Unhandled exception in sending message");
                    previousMessageFailed = false;
                }

                if (previousMessageFailed && retriesToGo <= 0) {
                    previousMessageFailed = false;
                }

                if (previousMessageFailed) {
                    Timber.i("Waiting for %s s before retrying", retryWait / 1000);
                    waitFuture = scheduler.schedule(() -> {
                        synchronized (locker) {
                            locker.notify();
                        }
                    }, retryWait, TimeUnit.MILLISECONDS);
                    synchronized (locker) {
                        locker.wait();
                    }
                    retryWait = Math.min(2 * retryWait, SEND_FAILURE_BACKOFF_MAX_WAIT);
                } else {
                    try {
                        if (!outgoingQueueIdlingResource.isIdleNow()) {
                            Timber.v("Decrementing outgoingQueueIdlingResource");
                            outgoingQueueIdlingResource.decrement();
                        }
                    } catch (IllegalStateException e) {
                        Timber.w(e, "outgoingQueueIdlingResource is invalid");
                    }
                }
            } catch (InterruptedException e) {
                Timber.i(e, "Outgoing message loop interrupted");
                break;
            }
        }
        scheduler.shutdown();
        Timber.w("Exiting outgoingmessage loop");
    }

    /**
     * Resets the retry backoff timer back to the initial value, because we've most likely had a
     * reconnection event.
     */
    public void notifyOutgoingMessageQueue() {
        if (waitFuture != null && waitFuture.cancel(false)) {
            Timber.d("Resetting message send loop wait.");
            retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
            synchronized (locker) {
                locker.notify();
            }
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(priority = 10, threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.EndpointChanged event) {
        acceptMessages = false;
        loadOutgoingMessageProcessor();
    }

    void onMessageDelivered() {
        endpointStateRepo.setQueueLength(outgoingQueue.size());
    }

    void onMessageDeliveryFailedFinal(String messageId) {
        Timber.e("Message delivery failed, not retryable. :%s", messageId);
        endpointStateRepo.setQueueLength(outgoingQueue.size());
    }

    void onMessageDeliveryFailed(String messageId) {
        Timber.e("Message delivery failed. queueLength: %s, messageId: %s", outgoingQueue.size() + 1, messageId);
        endpointStateRepo.setQueueLength(outgoingQueue.size());
    }

    void processIncomingMessage(MessageBase message) {
        Timber.i("Received incoming message: %s on %s", message.getClass().getSimpleName(), message.getContactKey());
        if (message instanceof MessageClear) {
            processIncomingMessage((MessageClear) message);
        } else if (message instanceof MessageLocation) {
            processIncomingMessage((MessageLocation) message);
        } else if (message instanceof MessageCard) {
            processIncomingMessage((MessageCard) message);
        } else if (message instanceof MessageCmd) {
            processIncomingMessage((MessageCmd) message);
        } else if (message instanceof MessageTransition) {
            processIncomingMessage((MessageTransition) message);
        }
    }

    private void processIncomingMessage(MessageClear message) {
        contactsRepo.remove(message.getContactKey());
    }

    private void processIncomingMessage(MessageLocation message) {
        // do not use TimeUnit.DAYS.toMillis to avoid long/double conversion issues...
        if ((preferences.getIgnoreStaleLocations() > 0) && (System.currentTimeMillis() - ((message).getTimestamp() * 1000)) > (preferences.getIgnoreStaleLocations() * 24 * 60 * 60 * 1000)) {
            Timber.e("discarding stale location");
            return;
        }
        contactsRepo.update(message.getContactKey(), message);
    }

    private void processIncomingMessage(MessageCard message) {
        contactsRepo.update(message.getContactKey(), message);
    }

    private void processIncomingMessage(MessageCmd message) {
        if (!preferences.getCmd()) {
            Timber.w("remote commands are disabled");
            return;
        }

        if (message.getModeId() != ConnectionMode.HTTP &&
                !preferences.getReceivedCommandsTopic().equals(message.getTopic())
        ) {
            Timber.e("cmd message received on wrong topic");
            return;
        }

        if (!message.isValidMessage()) {
            Timber.e("Invalid action message received");
            return;
        }
        if (message.getAction() != null) {
            switch (message.getAction()) {
                case REPORT_LOCATION:
                    if (message.getModeId() != ConnectionMode.MQTT) {
                        Timber.e("command not supported in HTTP mode: %s", message.getAction());
                        break;
                    }
                    serviceBridge.requestOnDemandLocationFix();

                    break;
                case WAYPOINTS:
                    locationProcessorLazy.get().publishWaypointsMessage();
                    break;
                case SET_WAYPOINTS:
                    if (message.getWaypoints() != null) {
                        waypointsRepo.importFromMessage(message.getWaypoints().getWaypoints());
                    }

                    break;
                case SET_CONFIGURATION:
                    if (!preferences.getRemoteConfiguration()) {
                        Timber.w("Received a remote configuration command but remote config setting is disabled");
                        break;
                    }
                    if (message.getConfiguration() != null) {
                        preferences.importConfiguration(message.getConfiguration());
                    } else {
                        Timber.w("No configuration provided");
                    }
                    if (message.getWaypoints() != null) {
                        waypointsRepo.importFromMessage(message.getWaypoints().getWaypoints());
                    }
                    break;
                case RECONNECT:
                    if (message.getModeId() != ConnectionMode.HTTP) {
                        Timber.e("command not supported in HTTP mode: %s", message.getAction());
                        break;
                    }
                    reconnect();
                    break;
                default:
                    break;
            }
        }
    }

    void publishLocationMessage(String trigger) {
        locationProcessorLazy.get().publishLocationMessage(trigger);
    }

    private void processIncomingMessage(MessageTransition message) {
        eventBus.post(message);
    }

    void stopSendingMessages() {
        Timber.d("Interrupting background sending thread");
        backgroundDequeueThread.interrupt();
    }

    @Override
    public void onPreferenceChanged(@NonNull List<String> properties) {
        if (properties.contains("mode")) {
            acceptMessages = false;
            loadOutgoingMessageProcessor();
        }
    }

    @NonNull
    public IdlingResource getMqttConnectionIdlingResource() {
        if (this.endpoint instanceof MQTTMessageProcessorEndpoint) {
            return ((MQTTMessageProcessorEndpoint) this.endpoint).getMqttConnectionIdlingResource();
        } else {
            return new SimpleIdlingResource("alwaysIdle", true);
        }
    }
}
