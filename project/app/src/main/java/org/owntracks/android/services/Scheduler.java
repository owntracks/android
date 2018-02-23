package org.owntracks.android.services;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;

import org.owntracks.android.App;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Scheduler extends SimpleJobService {
    public static final String BUNDLE_KEY_ACTION = "DISPATCHER_ACTION";
    public static final String BUNDLE_KEY_MESSAGE_ID = "MESSAGE_ID";

    public static final String ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING" ;
    private static final String PERIODIC_TASK_MQTT_KEEPALIVE = "PERIODIC_TASK_MQTT_KEEPALIVE" ;
    private static final String PERIODIC_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT";
    private static final String PERIODIC_TASK_PROCESS_QUEUE = "PERIODIC_TASK_PROCESS_QUEUE";


    private static Scheduler instance;
    private static FirebaseJobDispatcher dispatcher;

    public Scheduler() {
         dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(App.getContext()));
    }



    @Override
    public int onRunJob(JobParameters taskParams) {
        Bundle extras = taskParams.getExtras();
        if(extras == null) {
            Timber.e("Bundle extras are not set");
            return RESULT_FAIL_NORETRY;
        }

        String action = extras.getString(BUNDLE_KEY_ACTION);
        if(action == null) {
            Timber.e("BUNDLE_KEY_ACTION is not set");
            return RESULT_FAIL_NORETRY;
        }

        Timber.v("BUNDLE_KEY_ACTION: %s", extras.getString(BUNDLE_KEY_ACTION));

        switch (action) {
            //case ONEOFF_TASK_SEND_MESSAGE_HTTP:
             //   return MessageProcessorEndpointHttp.getInstance().sendMessage(extras);
           // case ONEOFF_TASK_SEND_MESSAGE_MQTT:
            //    return returnSuccess();// MessageProcessorEndpointMqtt.getInstance().sendMessage(extras);
            case PERIODIC_TASK_MQTT_KEEPALIVE:
                return MessageProcessorEndpointMqtt.getInstance().sendPing() ? returnSuccess() : returnFailRetry();
            case PERIODIC_TASK_MQTT_RECONNECT:
                return MessageProcessorEndpointMqtt.getInstance().checkConnection() ? returnSuccess() : returnFailRetry();
            case PERIODIC_TASK_SEND_LOCATION_PING:
                App.startBackgroundServiceCompat(this, BackgroundService.INTENT_ACTION_SEND_LOCATION_PING);
                return returnSuccess();
            case PERIODIC_TASK_PROCESS_QUEUE:
                Timber.v("processing queue");
                App.getMessageProcessor().processQueueHead();
                return returnSuccess();
            default:
                Timber.e("unknown BUNDLE_KEY_ACTION received: %s", action);
                return returnFailNoretry();
        }
    }



    public static int returnSuccess() {
        return RESULT_SUCCESS;
    }

    public static int returnFailRetry() {
        Timber.v("RESULT_FAIL_RETRY");
        return RESULT_FAIL_RETRY;
    }
    public static int returnFailNoretry() {
        Timber.v("RESULT_FAIL_NORETRY");
        return RESULT_FAIL_NORETRY;
    }

    public void cancelHttpTasks() {
        Timber.v("canceling tasks");
        dispatcher.cancel(ONEOFF_TASK_SEND_MESSAGE_HTTP);
    }

    public void cancelMqttTasks() {
        dispatcher.cancel(ONEOFF_TASK_SEND_MESSAGE_MQTT);
        dispatcher.cancel(PERIODIC_TASK_MQTT_KEEPALIVE);
        dispatcher.cancel(PERIODIC_TASK_MQTT_RECONNECT);
    }


    public boolean onStopJob(JobParameters job) {
        Timber.v("stoping job");
        // Remove stopd job from queue
        if(job.getExtras() != null) {
            App.getMessageProcessor().onMessageDeliveryFailedFinal(job.getExtras().getLong(BUNDLE_KEY_MESSAGE_ID));
        }
        return super.onStopJob(job);
    }

    public void scheduleMessage(Bundle b)  {
        if(b.get(BUNDLE_KEY_MESSAGE_ID) == null) {
            Timber.e("Bundle without BUNDLE_KEY_MESSAGE_ID");
            return;
        }
        if(b.get(BUNDLE_KEY_ACTION) == null) {
            Timber.e("Bundle without BUNDLE_KEY_ACTION");
            return;
        }

        Job job = dispatcher.newJobBuilder()
                .setService(Scheduler.class)
                .setTag(Long.toString(b.getLong(BUNDLE_KEY_MESSAGE_ID)))
                .setRecurring(false)
                //.setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                // Default linear retry strategy has a max backoff of 1h. Use custom prolicy with 30s to 10 minutes.
                .setRetryStrategy(dispatcher.newRetryStrategy(RetryStrategy.RETRY_POLICY_LINEAR, 30, 600))
                .setConstraints( Constraint.ON_ANY_NETWORK)
                //.setTrigger(Trigger.executionWindow(0, App.isInForeground() ? 0: 60))
                .setTrigger(Trigger.executionWindow(0,0))
                //.setTrigger(Trigger.NOW)
                .setReplaceCurrent(true)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setExtras(b)
                .build();

        Timber.v("scheduling task %s, %s", b.get(BUNDLE_KEY_ACTION), job.getTag());
        dispatcher.schedule(job);
    }

    public void scheduleMqttPing(long keepAliveSeconds) {

        Job job = dispatcher.newJobBuilder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_MQTT_KEEPALIVE)
                .setRecurring(true)
                .setRetryStrategy(dispatcher.newRetryStrategy(RetryStrategy.RETRY_POLICY_LINEAR, 30, 600))
                //.setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints( Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(0, (int)keepAliveSeconds))
                .setReplaceCurrent(true)
                .setExtras(getBundleForAction(PERIODIC_TASK_MQTT_KEEPALIVE))
                .build();

        Timber.v("scheduling task PERIODIC_TASK_MQTT_KEEPALIVE");
        dispatcher.schedule(job);
    }

    public void cancelMqttPing() {
        Timber.v("canceling task PERIODIC_TASK_MQTT_KEEPALIVE");
        dispatcher.cancel(PERIODIC_TASK_MQTT_KEEPALIVE);
    }

    public void cancelPeriodicQueueProcessing() {
        Timber.v("canceling task PERIODIC_TASK_PROCESS_QUEUE");
        dispatcher.cancel(PERIODIC_TASK_PROCESS_QUEUE);
    }


    public void scheduleLocationPing() {

        Job job = dispatcher.newJobBuilder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_SEND_LOCATION_PING)
                .setRecurring(true)
                .setRetryStrategy(dispatcher.newRetryStrategy(RetryStrategy.RETRY_POLICY_LINEAR, 30, 600))
                //.setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints( Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(30, (int)TimeUnit.MINUTES.toSeconds(App.getPreferences().getPing())))
                .setReplaceCurrent(true)
                .setExtras(getBundleForAction(PERIODIC_TASK_SEND_LOCATION_PING))
                .build();

        Timber.v("scheduling task PERIODIC_TASK_SEND_LOCATION_PING");

        dispatcher.schedule(job);
    }

    @NonNull
    public static Bundle getBundleForAction(String action) {
        Bundle b  = new Bundle();
        b.putString(BUNDLE_KEY_ACTION, action);
        return b;
    }

    public void scheduleMqttReconnect() {
        Job job = dispatcher.newJobBuilder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_MQTT_RECONNECT)
                .setRecurring(true)
                .setRetryStrategy(dispatcher.newRetryStrategy(RetryStrategy.RETRY_POLICY_LINEAR, 30, 600))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(0, (int)TimeUnit.MINUTES.toSeconds(10)))
                .setReplaceCurrent(true)
                .setExtras(getBundleForAction(PERIODIC_TASK_MQTT_RECONNECT))
                .build();

        Timber.v("scheduling task PERIODIC_TASK_MQTT_RECONNECT");
        dispatcher.schedule(job);
    }



    public void cancelMqttReconnect() {
        Timber.v("canceling task PERIODIC_TASK_MQTT_RECONNECT");
        dispatcher.cancel(PERIODIC_TASK_MQTT_RECONNECT);
    }


    public void scheduleQueueProcessing() {
        Job job = dispatcher.newJobBuilder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_PROCESS_QUEUE)
                .setRecurring(true)
                .setRetryStrategy(dispatcher.newRetryStrategy(RetryStrategy.RETRY_POLICY_LINEAR, 30, 600))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(0, (int)TimeUnit.MINUTES.toSeconds(10)))
                .setReplaceCurrent(true)
                .setExtras(getBundleForAction(PERIODIC_TASK_PROCESS_QUEUE))
                .build();

        Timber.v("scheduling task PERIODIC_TASK_PROCESS_QUEUE");
        dispatcher.schedule(job);
    }
}
