package org.owntracks.android.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.Geofence;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityFeatured;
import org.owntracks.android.activities.ActivityLauncher;
import org.owntracks.android.activities.ActivityStatus;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;
import org.owntracks.android.support.Toasts;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class ServiceNotification implements ProxyableService, StaticHandlerInterface {
    public static final String INTENT_ACTION_CANCEL_EVENT_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION";
    public static final String INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION";
    private static final String TAG ="ServiceNotification" ;

    private ServiceProxy context;
    private Preferences.OnPreferenceChangedListener preferencesChangedListener;
    private StaticHandler handler;
    private MessageLocation lastPublishedLocationMessage;
    private long lastPublishedLocationTst = 0;
    private NotificationManager notificationManager;

    // Ongoing notification
    private static final int NOTIFICATION_ID_ONGOING = 1;
    private NotificationCompat.Builder notificationBuilderOngoing;
    private Notification notificationOngoing;

    // Event notification
    private static final int NOTIFICATION_ID_EVENTS = 2;
    private NotificationCompat.Builder notificationBuilderEvents;
    private LinkedList<Spannable> notificationListEvents;
    private Notification notificationEvents;

    // Message notifications
    private NotificationCompat.Builder notificationBuilderMessages;
    private static final int NOTIFICATION_ID_MESSAGES = 3;
    private LinkedList<Spannable> notificationListMessages;
    private Notification notificationMessages;
    private SimpleDateFormat dateFormater;


    // Permission notification
    private NotificationCompat.Builder notificationBuilderPermission;
    private static final int NOTIFICATION_ID_PERMISSION = 4;
    private Notification notificationPermission;



    @Override
    public void onCreate(ServiceProxy c) {
        this.context = c;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationListMessages = new LinkedList<>();
        this.notificationListEvents = new LinkedList<>();


        this.dateFormater = new SimpleDateFormat("HH:mm", context.getResources().getConfiguration().locale);

        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        this.preferencesChangedListener = new Preferences.OnPreferenceChangedListener() {
            @Override
            public void onAttachAfterModeChanged() {
                clearNotifications();
                setupNotifications();
                updateNotifications();
            }

            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if (key.equals(Preferences.Keys.NOTIFICATION) || key.equals(Preferences.Keys.NOTIFICATION_LOCATION) || key.equals(Preferences.Keys.NOTIFICATION_EVENTS)) {
                    Log.v(TAG, "notification prefs changed");
                    clearNotifications();
                    setupNotifications();
                    updateNotifications();
                }
            }



        };

        this.handler = new StaticHandler(this);

        Preferences.registerOnPreferenceChangedListener(this.preferencesChangedListener);
        setupNotifications();
        updateNotifications();
    }



    private void clearNotifications() {
        if(!Preferences.getNotification())
            this.context.stopForeground(true);

        notificationManager.cancel(NOTIFICATION_ID_EVENTS);
        notificationManager.cancel(NOTIFICATION_ID_MESSAGES);

    }

    private void clearNotificationPermission() {
        notificationManager.cancel(NOTIFICATION_ID_PERMISSION);

    }

    private void setupNotifications() {

        setupNotificationOngoing();
        setupNotificationEvents();
    }



    private void updateNotifications() {
        updateNotificationOngoing();
        updateNotificationEvents();
    }

    /*
    * NOTIFICATION BUILDER SETUP
    * */
    private void setupNotificationOngoing() {
        Log.v(TAG, "setupNotificationOngoing. Enabled: " + Preferences.getNotification());
        if (!Preferences.getNotification())
            return;

        notificationBuilderOngoing = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityLauncher.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderOngoing.setContentIntent(resultPendingIntent);
        notificationBuilderOngoing.setSortKey("a");

        notificationBuilderOngoing.addAction(R.drawable.ic_report_notification, this.context.getString(R.string.publish), ServiceProxy.getPendingIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL, null, PendingIntent.FLAG_CANCEL_CURRENT));


    }


    @SuppressLint("NewApi")
    private void setupNotificationMessages() {
        if (!Preferences.getNotification())
            return;

        notificationBuilderMessages = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityFeatured.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilderMessages.setContentIntent(resultPendingIntent);
        notificationBuilderMessages.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION, null));
        notificationBuilderMessages.setSortKey("b");

        notificationBuilderMessages.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderMessages.setGroup(NOTIFICATION_ID_MESSAGES + "");
        notificationBuilderMessages.setAutoCancel(true);
        notificationBuilderMessages.setShowWhen(false);


        //if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderMessages.setColor(ContextCompat.getColor(context, R.color.primary));
            notificationBuilderMessages.setPriority(Notification.PRIORITY_MIN);
            notificationBuilderMessages.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderMessages.setVisibility(Notification.VISIBILITY_PUBLIC);
        //}

    }


    @SuppressLint("NewApi")
    private void setupNotificationEvents() {
        if (!Preferences.getNotificationEvents())
            return;

        notificationBuilderEvents = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityLauncher.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderEvents.setContentIntent(resultPendingIntent);
        notificationBuilderEvents.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEvents.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEvents.setAutoCancel(true);
        notificationBuilderEvents.setShowWhen(false);
        notificationBuilderEvents.setGroup(NOTIFICATION_ID_EVENTS + "");

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderEvents.setColor(ContextCompat.getColor(context, R.color.primary));
            notificationBuilderEvents.setPriority(Notification.PRIORITY_MIN);
            notificationBuilderEvents.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderEvents.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
    }

    public void  updateNotificationOngoing() {
        if (!Preferences.getNotification())
            return;

        String title;
        String subtitle = ServiceBroker.getStateAsString(this.context);
        long when = this.lastPublishedLocationTst;


        if (isLastPublishedLocationWithGeocoderAvailable() && Preferences.getNotificationLocation()) {
            title = this.lastPublishedLocationMessage.getGeocoder();
        } else {
            title = this.context.getString(R.string.app_name);
        }

        notificationBuilderOngoing.setContentTitle(title).setSmallIcon(R.drawable.ic_notification).setContentText(subtitle);
        notificationBuilderOngoing.setWhen(when);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderOngoing.setColor(context.getResources().getColor(R.color.primary));
            notificationBuilderOngoing.setPriority(Notification.PRIORITY_MIN);
            notificationBuilderOngoing.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderOngoing.setVisibility(Notification.VISIBILITY_PUBLIC);
        }



        this.notificationOngoing = notificationBuilderOngoing.build();
        this.context.startForeground(NOTIFICATION_ID_ONGOING, this.notificationOngoing);
    }


    public void addNotificationEvents(MessageTransition message) {

        FusedContact c = App.getFusedContact(message.getTopic());

        String name;
        String dateStr = dateFormater.format(new Date());
        String transition =  context.getString(message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionentering : R.string.transitionleaving);
        String location = message.getDesc();

        if (location == null) {
            location = context.getString(R.string.aLocation);
        }

        if(c != null)
            name = c.getFusedName();
        else {
            name = message.getTid();

            if (name == null) {
                name = message.getTopic();
            }
        }

        Spannable notification = new SpannableString(dateStr + ": " + name + " " + transition + " " + location);
        notification.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, dateStr.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        notificationListEvents.push(notification);
    }

    private void clearNotificationTransitions() {
        this.notificationListEvents.clear();
        notificationManager.cancel(NOTIFICATION_ID_EVENTS);

    }


    public void updateNotificationEvents() {
        if(!Preferences.getNotification() || !Preferences.getNotificationEvents() || this.notificationListEvents.size() == 0)
            return;


        InboxStyle style  = new InboxStyle();
        for (Spannable text : this.notificationListEvents)
            style.addLine(text);

        String title = context.getString(R.string.events);
        style.setBigContentTitle(title);

        notificationBuilderEvents.setStyle(style);
        notificationBuilderEvents.setContentText(this.notificationListEvents.getFirst());
        notificationBuilderEvents.setContentTitle(title);
        notificationBuilderEvents.setNumber(this.notificationListEvents.size());

        notificationManager.notify(NOTIFICATION_ID_EVENTS, notificationBuilderEvents.build());
    }


    public void clearNotificationMessages() {
        this.notificationListMessages.clear();
        notificationManager.cancel(NOTIFICATION_ID_MESSAGES);
    }

    public void updateNotificationMessage() {
        if(this.notificationListMessages.size() == 0)
            return;

        InboxStyle style = new NotificationCompat.InboxStyle();
        for (Spannable text : this.notificationListMessages)
            style.addLine(text);

        String title = context.getString(R.string.notificationMessageTitle);
        style.setBigContentTitle(title);

        context.getString(R.string.notificationMessageTitle);
        notificationBuilderMessages.setStyle(style);
        notificationBuilderMessages.setContentText(notificationListMessages.getFirst());
        notificationBuilderMessages.setContentTitle(title);
        notificationBuilderMessages.setNumber(notificationListMessages.size());

        notificationManager.notify(NOTIFICATION_ID_MESSAGES, notificationBuilderMessages.build());
    }


    @Override
    public void onDestroy() {
        clearNotificationMessages();
        clearNotificationTransitions();
    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {
        if (ServiceNotification.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION.equals(intent.getAction())) {
            clearNotificationMessages();
        } else if (ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION.equals(intent.getAction())) {
            clearNotificationTransitions();
        }
    }


    @Override
    public void handleHandlerMessage(Message msg) {
        switch (msg.what) {
            case ReverseGeocodingTask.GEOCODER_RESULT:
                geocoderAvailableForLocation(((GeocodableLocation) msg.obj));
                break;
        }

    }

    private void geocoderAvailableForLocation(GeocodableLocation l) {
    }

    private boolean isLastPublishedLocationWithGeocoderAvailable() {
        return this.lastPublishedLocationMessage != null && this.lastPublishedLocationMessage.getGeocoder() != null;
    }


    @Override
    public void onEvent(Events.Dummy event) { }




    @SuppressWarnings("unused")
    public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
        if (App.isInForeground())
            Toasts.showBrokerStateChange(e.getState());

        updateNotificationOngoing();
    }

    public void onEvent(Events.PermissionGranted e) {
        Log.v(TAG, "Events.PermissionGranted: " + e.getPermission() );
        if(e.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            clearNotificationPermission();
        }
    }


    public void onEvent(MessageLocation m) {
        Log.v(TAG, "onEvent MessageLocation");
        if(m.isOutgoing() && (lastPublishedLocationMessage == null || lastPublishedLocationMessage.getTst() <=  m.getTst())) {
            this.lastPublishedLocationMessage = m;
            Log.v(TAG, "resoving geocoder");
            GeocodingProvider.resolve(m, this);
        }
    }

    public void onMessageLocationGeocoderResult(MessageLocation m) {
        Log.v(TAG, "onMessageLocationGeocoderResult");

        if (m == lastPublishedLocationMessage) {
            Log.v(TAG, "updateNotificationOngoing");
            updateNotificationOngoing();
        }
    }

    public void processMessage(MessageTransition message) {
        if(message.getRetained())
            return;

        addNotificationEvents(message);
        updateNotificationEvents();

    }
    public void notifyMissingPermissions() {

        notificationBuilderPermission = new NotificationCompat.Builder(context);

        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        notificationBuilderPermission.setContentIntent(PendingIntent.getActivity(this.context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT));



        notificationBuilderPermission.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderPermission.setGroup(NOTIFICATION_ID_PERMISSION + "");
        notificationBuilderPermission.setAutoCancel(false);
        notificationBuilderPermission.setShowWhen(false);


        if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderPermission.setColor(ContextCompat.getColor(context, R.color.primary));
            notificationBuilderPermission.setPriority(Notification.PRIORITY_DEFAULT);
            notificationBuilderPermission.setCategory(Notification.CATEGORY_ERROR);
            notificationBuilderPermission.setVisibility(Notification.VISIBILITY_PUBLIC);
        }


        notificationBuilderPermission.setContentText("Missing a required permission");
        notificationBuilderPermission.setContentTitle(this.context.getString(R.string.app_name));

        notificationManager.notify(NOTIFICATION_ID_PERMISSION, notificationBuilderPermission.build());
    }
}

