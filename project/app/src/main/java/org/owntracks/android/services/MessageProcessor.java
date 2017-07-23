package org.owntracks.android.services;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Vibrator;
import android.util.LongSparseArray;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;


public class MessageProcessor implements IncomingMessageProcessor {
    private final EventBus eventBus;
    private final ContactsRepo contactsRepo;
    private final Preferences preferences;

    private final ThreadPoolExecutor incomingMessageProcessorExecutor;
    private final ThreadPoolExecutor outgoingMessageProcessorExecutor;
    private final Events.QueueChanged queueEvent = new Events.QueueChanged();
    private OutgoingMessageProcessor outgoingMessageProcessor;

    private boolean acceptMessages =  false;

    public void reconnect() {
        if(outgoingMessageProcessor instanceof StatefulServiceMessageProcessor)
            StatefulServiceMessageProcessor.class.cast(outgoingMessageProcessor).reconnect();
    }

    public void disconnect() {
        if(outgoingMessageProcessor instanceof StatefulServiceMessageProcessor)
            StatefulServiceMessageProcessor.class.cast(outgoingMessageProcessor).disconnect();
    }

    public void onEnterForeground() {
        if(outgoingMessageProcessor != null)
            outgoingMessageProcessor.onEnterForeground();
    }

    public void onEnterBackground() {
    }


    public int getQueueLength() {
        return outgoingQueue.size();
    }

    public boolean isEndpointConfigurationComplete() {
        return this.outgoingMessageProcessor != null && this.outgoingMessageProcessor.isConfigurationComplete();
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

    public MessageProcessor(EventBus eventBus, ContactsRepo contactsRepo, Preferences preferences) {
        this.preferences = preferences;
        this.eventBus = eventBus;
        this.contactsRepo = contactsRepo;

        this.incomingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        this.outgoingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        this.eventBus.register(this);
    }

    public void initialize() {
        onEndpointStateChanged(EndpointState.INITIAL);
        this.loadOutgoingMessageProcessor();
    }

    private void loadOutgoingMessageProcessor(){

        if(outgoingMessageProcessorExecutor != null) {
            outgoingMessageProcessorExecutor.purge();
        }

        if(outgoingMessageProcessor != null) {
            outgoingMessageProcessor.onDestroy();
        }

        outgoingQueue.clear();
        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));

        Timber.v("instantiating new outgoingMessageProcessorExecutor");
        switch (preferences.getModeId()) {
            case App.MODE_ID_HTTP_PRIVATE:
                this.outgoingMessageProcessor = MessageProcessorEndpointHttp.getInstance();
                break;
            case App.MODE_ID_MQTT_PRIVATE:
            case App.MODE_ID_MQTT_PUBLIC:
            default:
                this.outgoingMessageProcessor = MessageProcessorEndpointMqtt.getInstance();

        }
        this.outgoingMessageProcessor.onCreateFromProcessor();
        acceptMessages = true;
    }
    @SuppressWarnings("UnusedParameters")
    @Subscribe(priority = 10)
    public void onEvent(Events.ModeChanged event) {
        acceptMessages = false;
        loadOutgoingMessageProcessor();
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(priority = 10)
    public void onEvent(Events.EndpointChanged event) {
        acceptMessages = false;
        loadOutgoingMessageProcessor();
    }

    private final LongSparseArray<MessageBase> outgoingQueue = new LongSparseArray<>(10);

    public void sendMessage(MessageBase message) {
        if(!acceptMessages || !outgoingMessageProcessor.isConfigurationComplete()) return;

        message.setOutgoingProcessor(outgoingMessageProcessor);
        onMessageQueued(message);
        this.outgoingMessageProcessorExecutor.execute(message);
    }

     int onMessageDelivered(Long messageId) {
        MessageBase m = outgoingQueue.get(messageId);


        if(m != null) {
            // message will be treated as incoming message.
            // Set the delivered flag to distinguish it from messages received fro the broker.
            m.setDelivered(true);
            Timber.v("messageId:%s, queueLength:%s", messageId, outgoingQueue.size());
            if(m instanceof MessageLocation) {
                onMessageReceived(m);
                eventBus.post(m);
            }
            eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
            outgoingQueue.remove(messageId);

        } else {
            Timber.e("messageId:%s, queueLength:%s, error: unqueued, queue:%s", messageId, outgoingQueue.size(), outgoingQueue);
        }
        return Scheduler.RESULT_SUCCESS;
    }

    private void onMessageQueued(MessageBase m) {
        outgoingQueue.put(m.getMessageId(), m);
        eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
        Timber.v("messageId:%s, queueLength:%s, queue:%s", m.getMessageId(), outgoingQueue.size(), outgoingQueue);
    }

    int onMessageDeliveryFailed(Long messageId) {
        if(preferences.getDebugVibrate()) {
            Vibrator v = (Vibrator) App.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(2000);
        }

        MessageBase m = outgoingQueue.get(messageId);
        //outgoingQueue.remove(messageId);

        if(m == null) {
            Timber.e("messageId:%s, queueLength:%s, error: unqueued, queue:%s", messageId, outgoingQueue.size(), outgoingQueue);
            return Scheduler.RESULT_FAIL_NORETRY;
        } else {
            eventBus.postSticky(queueEvent.withNewLength(outgoingQueue.size()));
            if(m.getOutgoingTTL() > 0)  {
                if(!acceptMessages || !outgoingMessageProcessor.isConfigurationComplete()) {
                    Timber.e("messageId:%s, queueLength:%s, action: discarded/acceptMessages",m.getMessageId(),outgoingQueue.size() );
                    return Scheduler.RESULT_FAIL_NORETRY;
                }
                Timber.d("messageId:%s, queueLength:%s, action: requeued", m.getMessageId(), outgoingQueue.size());
                //onMessageQueued(m);
                return Scheduler.RESULT_FAIL_RETRY;

            } else {
                Timber.e("messageId:%s, queueLength:%s, action: discarded/expired",m.getMessageId(),outgoingQueue.size() );
                return Scheduler.RESULT_FAIL_NORETRY;
            }
        }
    }

    void onMessageReceived(MessageBase message) {
        //Timber.v("messageId:%s", message.getMessageId());
        message.setIncomingProcessor(this);
        incomingMessageProcessorExecutor.execute(message);
    }

    void onEndpointStateChanged(EndpointState newState) {
        App.getEventBus().postSticky(newState);
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
        contactsRepo.update(message.getContactKey(),message);

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


        if(!preferences.getPubTopicCommands().equals(message.getTopic())) {
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

                    Intent reportIntent = new Intent(App.getContext(), BackgroundService.class);
                    reportIntent.setAction(BackgroundService.INTENT_ACTION_SEND_LOCATION_RESPONSE);
                    App.getContext().startService(reportIntent);
                    break;
                case MessageCmd.ACTION_WAYPOINTS:
                    Intent waypointsIntent = new Intent(App.getContext(), BackgroundService.class);
                    waypointsIntent.setAction(BackgroundService.INTENT_ACTION_SEND_WAYPOINTS);
                    App.getContext().startService(waypointsIntent);
                    break;
                case MessageCmd.ACTION_SET_WAYPOINTS:
                    MessageWaypoints w = message.getWaypoints();
                    if (w != null)
                        preferences.importWaypointsFromJson(w.getWaypoints());
                    break;
                case MessageCmd.ACTION_SET_CONFIGURATION:
                    preferences.importFromMessage(message.getConfiguration());
                    break;
                case MessageCmd.ACTION_REOCONNECT:
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
