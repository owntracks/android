package org.owntracks.android.services;

import android.content.Context;
import android.content.res.Resources;
import android.util.LongSparseArray;

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
import org.owntracks.android.support.ServiceBridge;
import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
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

    private final ThreadPoolExecutor incomingMessageProcessorExecutor;
    private final ThreadPoolExecutor outgoingMessageProcessorExecutor;
    private final Events.QueueChanged queueEvent = new Events.QueueChanged();
    private final ServiceBridge serviceBridge;
    private MessageProcessorEndpoint endpoint;

    private boolean acceptMessages =  false;
    private final LongSparseArray<MessageBase> outgoingQueue = new LongSparseArray<>(10);

    public void reconnect() {
        if(endpoint == null)
            loadOutgoingMessageProcessor();

        if(endpoint instanceof StatefulServiceMessageProcessor)
            ((StatefulServiceMessageProcessor) endpoint).reconnect();
    }

    public boolean statefulSendKeepalive() {
        if(endpoint == null)
            loadOutgoingMessageProcessor();

        if(endpoint instanceof MessageProcessorEndpointMqtt)
            return ((MessageProcessorEndpointMqtt) endpoint).sendKeepalive();
        else
            return true;
    }

    public boolean statefulCheckConnection() {
        if(endpoint == null)
            loadOutgoingMessageProcessor();

        if(endpoint instanceof StatefulServiceMessageProcessor)
            return ((MessageProcessorEndpointMqtt) endpoint).checkConnection();
        else
            return true;
    }

    public boolean isEndpointConfigurationComplete() {
        return this.endpoint != null && this.endpoint.isConfigurationComplete();
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
        private Exception error;

        public String getMessage() {
            return message;
        }

        public Exception getError() {
            return error;
        }
        public EndpointState setMessage(String message) {
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

        public EndpointState setError(Exception error) {
            this.error = error;
            return this;
        }
    }

    @Inject
    public MessageProcessor(EventBus eventBus, ContactsRepo contactsRepo, Preferences preferences, WaypointsRepo waypointsRepo, Parser parser, Scheduler scheduler, Lazy<LocationProcessor> locationProcessorLazy, ServiceBridge serviceBridge) {
        this.preferences = preferences;
        this.eventBus = eventBus;
        this.contactsRepo = contactsRepo;
        this.waypointsRepo = waypointsRepo;
        this.parser = parser;
        this.scheduler = scheduler;
        this.locationProcessorLazy = locationProcessorLazy;
        this.serviceBridge = serviceBridge;
        this.incomingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<>());
        this.outgoingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<>());
        this.eventBus.register(this);
    }

    public void initialize() {
        onEndpointStateChanged(EndpointState.INITIAL);
        loadOutgoingMessageProcessor();
    }

    private void loadOutgoingMessageProcessor(){

        if(outgoingMessageProcessorExecutor != null) {
            outgoingMessageProcessorExecutor.purge();
        }

        if(endpoint != null) {
            endpoint.onDestroy();
        }

        outgoingQueue.clear();
        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));

        Timber.v("instantiating new outgoingMessageProcessorExecutor");
        switch (preferences.getModeId()) {
            case MessageProcessorEndpointHttp.MODE_ID:
                this.endpoint = new MessageProcessorEndpointHttp(this, this.parser, this.preferences, this.scheduler, this.eventBus);
                break;
            case MessageProcessorEndpointMqtt.MODE_ID:
            default:
                this.endpoint = new MessageProcessorEndpointMqtt(this, this.parser, this.preferences, this.scheduler, this.eventBus);

        }
        this.endpoint.onCreateFromProcessor();
        acceptMessages = true;
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


    public void sendMessage(MessageBase message) {
        if(!acceptMessages || !endpoint.isConfigurationComplete()) return;

        outgoingQueue.put(message.getMessageId(), message);
        Timber.v("messageId:%s, queueLength:%s, queue:%s", message.getMessageId(), outgoingQueue.size(), outgoingQueue);
        processQueueHead();
    }

     void onMessageDelivered(Long messageId) {
        MessageBase m = outgoingQueue.get(messageId);


        if(m != null) {
            // message will be treated as incoming message.
            // Set the delivered flag to distinguish it from messages received fro the broker.
            m.setDelivered(true);
            Timber.v("messageId:%s, queueLength:%s", messageId, outgoingQueue.size());
            if(m instanceof MessageLocation) {
                endpoint.onMessageReceived(m);
                eventBus.post(m);
            }
            dequeue(m.getMessageId());

        } else {
            Timber.e("messageId:%s, queueLength:%s, error: unqueued, queue:%s", messageId, outgoingQueue.size(), outgoingQueue);
        }
        processQueueHead();
    }

    public synchronized void processQueueHead() {
        MessageBase head = outgoingQueue.get(outgoingQueue.keyAt(0));
        if (head == null) {
            Timber.v("queue empty");
            eventBus.postSticky(queueEvent.withNewLength(0));

            return;
        }
        if (head.isOutgoing()) {
            Timber.e("queue is already processing");
           return;
        }
        Timber.v("getting first message from queue: %s", head.getMessageId());

        head.setOutgoingProcessor(endpoint);
        this.outgoingMessageProcessorExecutor.execute(head);

    }

    public void onMessageDeliveryFailedFinal(Long messageId) {
        Timber.e(":%s", messageId);
        dequeue(messageId);
        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
    }

    private void dequeue(long messageId) {
        Timber.v("messageId:%s", messageId);
        outgoingQueue.remove(messageId);
    }

    void onMessageDeliveryFailed(Long messageId) {
        Timber.e("queueLength: %s, messageId: %s", outgoingQueue.size(),messageId);

        MessageBase m = outgoingQueue.get(messageId);

         if(m != null) {
             m.clearOutgoingProcessor();
         }


        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
    }


    void onMessageReceived(MessageBase message) {
        message.setIncomingProcessor(this);
        incomingMessageProcessorExecutor.execute(message);
    }

    void onEndpointStateChanged(EndpointState newState) {
        Timber.v("message:%s, ", newState.getMessage());
        eventBus.postSticky(newState);
    }

    @Override
    public void processIncomingMessage(MessageBase message) {
        Timber.v("type:base, key:%s", message.getContactKey());
    }

    public void processIncomingMessage(MessageUnknown message) {
        Timber.v("type:unknown, key:%s", message.getContactKey());
    }

    @Override
    public void processIncomingMessage(MessageClear message) {

        contactsRepo.remove(message.getContactKey());
    }


    @Override
    public void processIncomingMessage(MessageLocation message) {
        Timber.v("processing location message %s", message.getContactKey());
        if ((preferences.getIgnoreStaleLocations() > 0) && (System.currentTimeMillis() - (message.getTst() * 1000)) > TimeUnit.DAYS.toMillis(preferences.getIgnoreStaleLocations())) {
            Timber.e("discarding stale location");
            return;
        }
        contactsRepo.update(message.getContactKey(), message);
    }

    @Override
    public void processIncomingMessage(MessageCard message) {
        contactsRepo.update(message.getContactKey(),message);
    }

    @Override
    public void processIncomingMessage(MessageCmd message) {
        if(!preferences.getRemoteCommand()) {
            Timber.e("remote commands are disabled");
            return;
        }

        if(message.getModeId() != MessageProcessorEndpointHttp.MODE_ID &&  !preferences.getPubTopicCommands().equals(message.getTopic())) {
            Timber.e("cmd message received on wrong topic");
            return;
        }


        String actions = message.getAction();
        if(actions == null) {
            Timber.e("no action in cmd message");
            return;
        }

        for(String cmd : actions.split(",")) {

            switch (cmd) {
                case MessageCmd.ACTION_REPORT_LOCATION:
                    if(message.getModeId() != MessageProcessorEndpointMqtt.MODE_ID) {
                        Timber.e("command not supported in HTTP mode: %s", cmd);
                        break;
                    }
                    serviceBridge.requestOnDemandLocationFix();

                    break;
                case MessageCmd.ACTION_WAYPOINTS:
                    locationProcessorLazy.get().publishWaypointsMessage();
                    break;
                case MessageCmd.ACTION_SET_WAYPOINTS:
                    if(message.getWaypoints() != null) {
                        waypointsRepo.importFromMessage(message.getWaypoints().getWaypoints());
                    }

                    break;
                case MessageCmd.ACTION_SET_CONFIGURATION:
                    preferences.importFromMessage(message.getConfiguration());
                    if(message.getWaypoints() != null) {
                        waypointsRepo.importFromMessage(message.getWaypoints().getWaypoints());
                    }
                    break;
                case MessageCmd.ACTION_RECONNECT:
                    if(message.getModeId() != MessageProcessorEndpointHttp.MODE_ID) {
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
}
