package org.owntracks.android.services;

import android.content.Context;
import android.content.res.Resources;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.ServiceBridge;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import timber.log.Timber;

@PerApplication
public class MessageProcessor implements IncomingMessageProcessor {
    private final EventBus eventBus;
    private final ContactsRepo contactsRepo;
    private final WaypointsRepo waypointsRepo;
    private final Preferences preferences;
    private final Parser parser;
    private final Scheduler scheduler;
    private final Lazy<LocationProcessor> locationProcessorLazy;

    private final Events.QueueChanged queueEvent = new Events.QueueChanged();
    private final ServiceBridge serviceBridge;
    private final RunThingsOnOtherThreads runThingsOnOtherThreads;
    private MessageProcessorEndpoint endpoint;

    private boolean acceptMessages = false;
    private final BlockingDeque<MessageBase> outgoingQueue = new LinkedBlockingDeque<>(100);
    private Thread backgroundDequeueThread;

    private static final long SEND_FAILURE_BACKOFF_INITIAL_WAIT = TimeUnit.SECONDS.toMillis(1);
    private static final long SEND_FAILURE_BACKOFF_MAX_WAIT = TimeUnit.MINUTES.toMillis(1);

    @Inject
    public MessageProcessor(EventBus eventBus, ContactsRepo contactsRepo, Preferences preferences, WaypointsRepo waypointsRepo, Parser parser, Scheduler scheduler, Lazy<LocationProcessor> locationProcessorLazy, ServiceBridge serviceBridge, RunThingsOnOtherThreads runThingsOnOtherThreads) {
        this.preferences = preferences;
        this.eventBus = eventBus;
        this.contactsRepo = contactsRepo;
        this.waypointsRepo = waypointsRepo;
        this.parser = parser;
        this.scheduler = scheduler;
        this.locationProcessorLazy = locationProcessorLazy;
        this.serviceBridge = serviceBridge;
        this.eventBus.register(this);
        this.runThingsOnOtherThreads = runThingsOnOtherThreads;
    }

    public void initialize() {
        onEndpointStateChanged(EndpointState.INITIAL);
        reconnect();
    }

    public void reconnect() {
        reconnect(null);
    }

    public void reconnect(Semaphore completionNotifier) {
        if (endpoint == null) {
            loadOutgoingMessageProcessor(); // The processor should take care of the reconnect on init
        } else if (endpoint instanceof MessageProcessorEndpointMqtt) {
            ((MessageProcessorEndpointMqtt) endpoint).reconnect(completionNotifier);
        } else if (completionNotifier != null) {
            // This is not an MQTT endpoint, but we've been given a notifier. Just release it.
            completionNotifier.release();
        }
    }

    public boolean statefulReconnectAndSendKeepalive() {
        if (endpoint == null)
            loadOutgoingMessageProcessor();

        if (endpoint instanceof MessageProcessorEndpointMqtt) {
            Semaphore lock = new Semaphore(1);
            lock.acquireUninterruptibly();
            ((MessageProcessorEndpointMqtt) endpoint).reconnectAndSendKeepalive(lock);
            try {
                lock.acquire();
                return true;
            } catch (InterruptedException e) {
                Timber.w(e, "Interrupted waiting for reconnect future to complete");
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean statefulCheckConnection() {
        if (endpoint == null)
            loadOutgoingMessageProcessor();

        if (endpoint instanceof StatefulServiceMessageProcessor)
            return ((StatefulServiceMessageProcessor) endpoint).checkConnection();
        else
            return true;
    }

    public boolean isEndpointConfigurationComplete() {
        try {
            if (this.endpoint != null) {
                this.endpoint.checkConfigurationComplete();
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
            endpoint.onDestroy();
        }

        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));

        switch (preferences.getMode()) {
            case MessageProcessorEndpointHttp.MODE_ID:
                this.endpoint = new MessageProcessorEndpointHttp(this, this.parser, this.preferences, this.scheduler, this.eventBus);
                break;
            case MessageProcessorEndpointMqtt.MODE_ID:
            default:
                this.endpoint = new MessageProcessorEndpointMqtt(this, this.parser, this.preferences, this.scheduler, this.eventBus, this.runThingsOnOtherThreads);

        }

        if (backgroundDequeueThread == null || !backgroundDequeueThread.isAlive()) {
            // Create the background thread that will handle outbound msgs
            backgroundDequeueThread = new Thread(this::sendAvailableMessages);
            backgroundDequeueThread.start();
        }

        this.endpoint.onCreateFromProcessor();
        acceptMessages = true;
    }

    public void queueMessageForSending(MessageBase message) {
        if (!acceptMessages) return;
        Timber.d("Queueing messageId:%s, queueLength:%s, ThreadID: %s", message.getMessageId(), outgoingQueue.size(), Thread.currentThread());
        synchronized (outgoingQueue) {
            if (!outgoingQueue.offer(message)) {
                MessageBase droppedMessage = outgoingQueue.poll();
                Timber.e("Outoing queue full. Dropping oldest message: %s", droppedMessage);
                if (!outgoingQueue.offer(message)) {
                    Timber.e("Still can't put message onto the queue. Dropping: %s", message);
                }
            }
        }
    }

    // Should be on the background thread here, because we block
    private void sendAvailableMessages() {
        Timber.d("Starting outbound message loop. ThreadID: %s", Thread.currentThread());
        MessageBase lastFailedMessageToBeRetried = null;
        long retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
        while (true) {
            try {
                MessageBase message;
                if (lastFailedMessageToBeRetried == null) {
                    message = this.outgoingQueue.take(); // <--- blocks
                } else {
                    message = lastFailedMessageToBeRetried;
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
                    lastFailedMessageToBeRetried = null;
                    retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
                } catch (OutgoingMessageSendingException | ConfigurationIncompleteException e) {
                    Timber.w(("Error sending message. Re-queueing"));
                    lastFailedMessageToBeRetried = message;
                } catch (IOException e) {
                    retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
                    // Deserialization failure, drop and move on
                } catch (Throwable e) {
                    Timber.e(e, "Unhandled exception in sending message");
                }
                if (lastFailedMessageToBeRetried != null) {
                    Thread.sleep(retryWait);
                    retryWait = Math.min(2 * retryWait, SEND_FAILURE_BACKOFF_MAX_WAIT);
                }
            } catch (InterruptedException e) {
                Timber.i(e, "Outgoing message loop interrupted");
                break;
            }
        }
        Timber.w("Exiting outgoingmessage loop");
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(priority = 10, threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.ModeChanged event) {
        acceptMessages = false;
        loadOutgoingMessageProcessor();
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(priority = 10, threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.EndpointChanged event) {
        acceptMessages = false;
        loadOutgoingMessageProcessor();
    }

    void onMessageDelivered(MessageBase messageBase) {
        Timber.d("onMessageDelivered in MessageProcessor Noop. ThreadID: %s", Thread.currentThread());
        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
        eventBus.post(messageBase);
    }

    void onMessageDeliveryFailedFinal(Long messageId) {
        Timber.e("Message delivery failed, not retryable. :%s", messageId);
        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
    }

    void onMessageDeliveryFailed(Long messageId) {
        Timber.e("Message delivery failed. queueLength: %s, messageId: %s", outgoingQueue.size(), messageId);
        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
    }

    void onEndpointStateChanged(EndpointState newState) {
        Timber.d("message:%s, ", newState.getMessage());
        eventBus.postSticky(newState);
    }

    @Override
    public void processIncomingMessage(MessageBase message) {
        Timber.d("type:base, key:%s", message.getContactKey());
    }

    public void processIncomingMessage(MessageUnknown message) {
        Timber.i("type:unknown, key:%s", message.getContactKey());
    }

    @Override
    public void processIncomingMessage(MessageClear message) {
        contactsRepo.remove(message.getContactKey());
    }

    @Override
    public void processIncomingMessage(MessageLocation message) {
        Timber.d("processing location message %s. ThreadID: %s", message.getContactKey(), Thread.currentThread());
        // do not use TimeUnit.DAYS.toMillis to avoid long/double conversion issues...
        if ((preferences.getIgnoreStaleLocations() > 0) && (System.currentTimeMillis() - (message.getTst() * 1000)) > (preferences.getIgnoreStaleLocations() * 24 * 60 * 60 * 1000)) {
            Timber.e("discarding stale location");
            return;
        }
        contactsRepo.update(message.getContactKey(), message);
    }

    @Override
    public void processIncomingMessage(MessageCard message) {
        contactsRepo.update(message.getContactKey(), message);
    }

    @Override
    public void processIncomingMessage(MessageCmd message) {
        if (!preferences.getRemoteCommand()) {
            Timber.w("remote commands are disabled");
            return;
        }

        if (message.getModeId() != MessageProcessorEndpointHttp.MODE_ID && !preferences.getPubTopicCommands().equals(message.getTopic())) {
            Timber.e("cmd message received on wrong topic");
            return;
        }

        String actions = message.getAction();
        if (actions == null) {
            Timber.e("no action in cmd message");
            return;
        }

        for (String cmd : actions.split(",")) {
            switch (cmd.trim()) {
                case MessageCmd.ACTION_REPORT_LOCATION:
                    if (message.getModeId() != MessageProcessorEndpointMqtt.MODE_ID) {
                        Timber.e("command not supported in HTTP mode: %s", cmd);
                        break;
                    }
                    serviceBridge.requestOnDemandLocationFix();

                    break;
                case MessageCmd.ACTION_WAYPOINTS:
                    locationProcessorLazy.get().publishWaypointsMessage();
                    break;
                case MessageCmd.ACTION_SET_WAYPOINTS:
                    if (message.getWaypoints() != null) {
                        waypointsRepo.importFromMessage(message.getWaypoints().getWaypoints());
                    }

                    break;
                case MessageCmd.ACTION_SET_CONFIGURATION:
                    preferences.importFromMessage(message.getConfiguration());
                    if (message.getWaypoints() != null) {
                        waypointsRepo.importFromMessage(message.getWaypoints().getWaypoints());
                    }
                    break;
                case MessageCmd.ACTION_RECONNECT:
                    if (message.getModeId() != MessageProcessorEndpointHttp.MODE_ID) {
                        Timber.e("command not supported in HTTP mode: %s", cmd);
                        break;
                    }
                    reconnect();
                    break;
                case MessageCmd.ACTION_RESTART:
                    App.restart();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void processIncomingMessage(MessageTransition message) {
        eventBus.post(message);
    }

    public enum EndpointState {
        INITIAL,
        IDLE,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        DISCONNECTED_USERDISCONNECT,
        ERROR,
        ERROR_DATADISABLED,
        ERROR_CONFIGURATION;

        String message;
        private Throwable error;

        public String getMessage() {
            if (message == null) {
                if (error != null) {
                    if (error instanceof MqttException && error.getCause() != null)
                        return String.format("MQTT Error: %s", error.getMessage());
                    else
                        return error.getMessage();
                } else {
                    return null;
                }
            }
            return message;
        }

        public Throwable getError() {
            return error;
        }

        public EndpointState withMessage(String message) {
            this.message = message;
            return this;
        }


        public String getLabel(Context context) {
            Resources res = context.getResources();
            int resId = res.getIdentifier(this.name(), "string", context.getPackageName());
            if (0 != resId) {
                return (res.getString(resId));
            }
            return (name());
        }

        public EndpointState withError(Throwable error) {
            this.error = error;
            return this;
        }
    }
}
