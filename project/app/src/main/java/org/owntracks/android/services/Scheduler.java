package org.owntracks.android.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;

import org.owntracks.android.App;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.support.Preferences;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Scheduler extends SimpleJobService {
    public static final String BUNDLE_KEY_ACTION = "DISPATCHER_ACTION";
    public static final String BUNDLE_KEY_MESSAGE_ID = "MESSAGE_ID";

    public static final String ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING" ;
    private static final String PERIODIC_TASK_MQTT_PING = "PERIODIC_TASK_MQTT_PING" ;
    private static final String PERIODIC_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT";
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
            case ONEOFF_TASK_SEND_MESSAGE_HTTP:
                return MessageProcessorEndpointHttp.getInstance().sendMessage(extras);
            case ONEOFF_TASK_SEND_MESSAGE_MQTT:
                return MessageProcessorEndpointMqtt.getInstance().sendMessage(extras);
            case PERIODIC_TASK_MQTT_PING:
                return MessageProcessorEndpointMqtt.getInstance().sendPing() ? returnSuccess() : returnFailRetry();
            case PERIODIC_TASK_MQTT_RECONNECT:
                return MessageProcessorEndpointMqtt.getInstance().checkConnection() ? returnSuccess() : returnFailRetry();
            case PERIODIC_TASK_SEND_LOCATION_PING:
                App.startBackgroundServiceCompat(this, BackgroundService.INTENT_ACTION_SEND_LOCATION_PING);
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
        return RESULT_FAIL_NORETRY;
    }
    public static int returnFailNoretry() {
        return RESULT_FAIL_NORETRY;
    }

    public void cancelHttpTasks() {
        dispatcher.cancel(ONEOFF_TASK_SEND_MESSAGE_HTTP);
    }

    public void cancelMqttTasks() {
        dispatcher.cancel(ONEOFF_TASK_SEND_MESSAGE_MQTT);
        dispatcher.cancel(PERIODIC_TASK_MQTT_PING);
        dispatcher.cancel(PERIODIC_TASK_MQTT_RECONNECT);
    }


    public boolean onStopJob(JobParameters job) {
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
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
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
                .setTag(PERIODIC_TASK_MQTT_PING)
                .setRecurring(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints( Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(0, (int)keepAliveSeconds))
                .setReplaceCurrent(true)
                .setExtras(getBundleForAction(PERIODIC_TASK_MQTT_PING))
                .build();

        Timber.v("scheduling task PERIODIC_TASK_MQTT_PING");
        dispatcher.schedule(job);
    }

    public void cancelMqttPing() {
        Timber.v("canceling task PERIODIC_TASK_MQTT_PING");
        dispatcher.cancel(PERIODIC_TASK_MQTT_PING);
    }

    public void scheduleLocationPing() {

        Job job = dispatcher.newJobBuilder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_SEND_LOCATION_PING)
                .setRecurring(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints( Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(30, (int)TimeUnit.MINUTES.toSeconds(Preferences.getPing())))
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
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
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


}
