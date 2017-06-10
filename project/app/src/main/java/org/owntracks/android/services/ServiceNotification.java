package org.owntracks.android.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityWelcome;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.widgets.Toasts;
import org.owntracks.android.support.interfaces.ProxyableService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ServiceNotification implements ProxyableService {
    public static final String INTENT_ACTION_CANCEL_EVENT_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION";
    public static final String INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION"; //unused for now
    private static final String TAG ="ServiceNotification" ;
    private static final String GROUP_KEY_EVENTS = "events";

    private ServiceProxy context;
    private Preferences.OnPreferenceChangedListener preferencesChangedListener;
    private NotificationManagerCompat notificationManager;
    private SimpleDateFormat dateFormater;

    // Ongoing notification
    private static final int NOTIFICATION_ID_ONGOING = 1;
    private NotificationCompat.Builder notificationBuilderOngoing;
    private MessageLocation notificationOngoingLastLocationCache;
    private MessageProcessor.EndpointState notificationOngoingLastStateCache = MessageProcessor.EndpointState.INITIAL;

    // Event notification
    private static final int NOTIFICATION_ID_EVENTS_GROUP = 2;
    private int notificationIdEvents = 3;
    private NotificationCompat.Builder notificationBuilderEvents;
    private NotificationCompat.Builder notificationBuilderEventsGroup;

    private Notification notificationEventsSummary;

    // Permission notification
    private NotificationCompat.Builder notificationBuilderPermission;
    private static final int NOTIFICATION_ID_PERMISSION = 4;



    @Override
    public void onCreate(ServiceProxy c) {
        this.context = c;

        this.notificationManager = NotificationManagerCompat.from(context);
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

        Preferences.registerOnPreferenceChangedListener(this.preferencesChangedListener);
        setupNotifications();
        updateNotifications();
    }



    private void clearNotifications() {
        if(!Preferences.getNotification())
            this.context.stopForeground(true);

        notificationManager.cancel(NOTIFICATION_ID_EVENTS_GROUP);

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

        Intent resultIntent = new Intent(this.context, ActivityWelcome.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderOngoing.setContentIntent(resultPendingIntent);
        notificationBuilderOngoing.setSortKey("a");

        notificationBuilderOngoing.addAction(R.drawable.ic_report_notification, this.context.getString(R.string.publish), ServiceProxy.getPendingIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL, null, PendingIntent.FLAG_CANCEL_CURRENT));

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            notificationBuilderOngoing.setColor(context.getResources().getColor(R.color.primary, context.getTheme()));
            notificationBuilderOngoing.setCategory(NotificationCompat.CATEGORY_SERVICE);
            notificationBuilderOngoing.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        notificationBuilderOngoing.setOngoing(true);

    }

    @SuppressLint("NewApi")
    private void setupNotificationEvents() {
        if (!Preferences.getNotificationEvents())
            return;

        notificationBuilderEvents = new NotificationCompat.Builder(context);
        notificationBuilderEventsGroup = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityWelcome.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderEvents.setContentIntent(resultPendingIntent);
        notificationBuilderEvents.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEvents.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEvents.setAutoCancel(true);
        notificationBuilderEvents.setShowWhen(true);
        notificationBuilderEvents.setGroup(NOTIFICATION_ID_EVENTS_GROUP + "");


        notificationBuilderEventsGroup.setContentIntent(resultPendingIntent);
        notificationBuilderEventsGroup.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEventsGroup.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEventsGroup.setAutoCancel(true);
        notificationBuilderEventsGroup.setShowWhen(true);
        notificationBuilderEventsGroup.setGroup(NOTIFICATION_ID_EVENTS_GROUP + "");
        notificationBuilderEventsGroup.setGroupSummary(true);

        notificationBuilderEvents.setColor(ContextCompat.getColor(context, R.color.primary));
        notificationBuilderEvents.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationBuilderEvents.setCategory(NotificationCompat.CATEGORY_SERVICE);
        notificationBuilderEvents.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationBuilderEventsGroup.setColor(ContextCompat.getColor(context, R.color.primary));
        notificationBuilderEventsGroup.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationBuilderEventsGroup.setCategory(NotificationCompat.CATEGORY_SERVICE);
        notificationBuilderEventsGroup.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);



    }
    public void  updateNotificationOngoing() {
        if (!Preferences.getNotification())
            return;

        Timber.v("enabled:%s, state:%s", Preferences.getNotification(), notificationOngoingLastStateCache.getLabel(context));


        String subtitle = notificationOngoingLastStateCache.getLabel(context);

        if (isLastPublishedLocationWithGeocoderAvailable() && Preferences.getNotificationLocation()) {
            notificationBuilderOngoing.setContentTitle(this.notificationOngoingLastLocationCache.getGeocoder());
            notificationBuilderOngoing.setWhen(TimeUnit.SECONDS.toMillis(this.notificationOngoingLastLocationCache.getTst()));

        } else {
            notificationBuilderOngoing.setContentTitle(this.context.getString(R.string.app_name));
        }


        if(Preferences.getNotificationHigherPriority())
            notificationBuilderOngoing.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        else
            notificationBuilderOngoing.setPriority(NotificationCompat.PRIORITY_MIN);

        notificationBuilderOngoing.setSmallIcon(R.drawable.ic_notification).setContentText(subtitle);
        this.context.startForeground(NOTIFICATION_ID_ONGOING, notificationBuilderOngoing.build());
    }


    public void addNotificationEvents(MessageTransition message) {
        if (!Preferences.getNotificationEvents())
            return;

        // Prepare data
        FusedContact c = App.getFusedContact(message.getContactKey());

        String name;
        long when = message.getTst()*1000;
        String dateStr = dateFormater.format(new Date(when));
        String transition =  context.getString(message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionEntering : R.string.transitionLeaving);
        String location = message.getDesc();

        if (location == null) {
            location = context.getString(R.string.aLocation);
        }

        if(c != null)
            name = c.getFusedName();
        else {
            name = message.getTid();

            if (name == null) {
                name = message.getContactKey();
            }
        }

        if(notificationBuilderEvents == null || notificationBuilderEventsGroup == null)
            setupNotificationEvents();

        // Add single notification
        notificationBuilderEvents.setContentTitle(name);
        notificationBuilderEvents.setContentText(transition + " " + location);
        notificationBuilderEvents.setWhen(when);
        notificationBuilderEvents.setShowWhen(true);
        notificationManager.notify(++notificationIdEvents, notificationBuilderEvents.build());


        // Add group notification
        android.support.v4.app.NotificationCompat.InboxStyle style  = new android.support.v4.app.NotificationCompat.InboxStyle();

        // Append new notification to existing
        if(notificationEventsSummary != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            CharSequence cs[] = (CharSequence[]) notificationEventsSummary.extras.get(NotificationCompat.EXTRA_TEXT_LINES);
            for (CharSequence line : cs != null ? cs : new CharSequence[0]) {
                style.addLine(line);
            }
            
            notificationBuilderEventsGroup.setNumber(cs.length+1);
        }

        Spannable newLine = new SpannableString(name + " " + transition + " " + location);
        newLine.setSpan(new StyleSpan(Typeface.BOLD), 0, dateStr.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        style.addLine(newLine);
        String title = context.getString(R.string.events);
        style.setBigContentTitle(title);

        notificationBuilderEventsGroup.setStyle(style);
        notificationBuilderEventsGroup.setContentTitle(title);
        notificationBuilderEventsGroup.setWhen(when);



        notificationEventsSummary = notificationBuilderEventsGroup.build();
        notificationManager.notify(NOTIFICATION_ID_EVENTS_GROUP, notificationEventsSummary);


    }

    private void clearNotificationEvents() {
        notificationManager.cancel(NOTIFICATION_ID_EVENTS_GROUP);
        notificationIdEvents = NOTIFICATION_ID_EVENTS_GROUP+1;
    }


    private void updateNotificationEvents() {
        if(!Preferences.getNotification() || !Preferences.getNotificationEvents())
            clearNotificationEvents();
    }


    @Override
    public void onDestroy() {
        clearNotifications();
        Preferences.unregisterOnPreferenceChangedListener(this.preferencesChangedListener);
    }

    @Override
    public void onStartCommand(Intent intent) {
        if (ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION.equals(intent.getAction())) {
            clearNotificationEvents();
        }
    }



    private boolean isLastPublishedLocationWithGeocoderAvailable() {
        return this.notificationOngoingLastLocationCache != null && this.notificationOngoingLastLocationCache.getGeocoder() != null;
    }


    @Subscribe
    public void onEvent(Events.Dummy event) { }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.ModeChanged e) {
        updateNotificationOngoing();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEventMainThread(Events.EndpointStateChanged e) {
        Timber.v("EndpointStateChanged %s", e.getState().getLabel(context));
        notificationOngoingLastStateCache = e.getState();
        updateNotificationOngoing();
    }

    @Subscribe
    public void onEvent(Events.PermissionGranted e) {
        Timber.v("Events.PermissionGranted: %s", e.getPermission() );
        if(e.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            clearNotificationPermission();
        }
    }

    @Subscribe
    public void onEvent(MessageLocation m) {
        if(m.isOutgoing() && (notificationOngoingLastLocationCache == null || notificationOngoingLastLocationCache.getTst() <=  m.getTst())) {
            this.notificationOngoingLastLocationCache = m;
            App.getGeocodingProvider().resolve(m, this);
        }
    }

    public void onMessageLocationGeocoderResult(MessageLocation m) {
        if (m == notificationOngoingLastLocationCache) {
            updateNotificationOngoing();
        }
    }

    public void processMessage(MessageTransition message) {
        if(message.getRetained())
            return;

        addNotificationEvents(message);

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
            notificationBuilderPermission.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notificationBuilderPermission.setCategory(NotificationCompat.CATEGORY_ERROR);
            notificationBuilderPermission.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }


        notificationBuilderPermission.setContentText("Missing a required permission");
        notificationBuilderPermission.setContentTitle(this.context.getString(R.string.app_name));

        notificationManager.notify(NOTIFICATION_ID_PERMISSION, notificationBuilderPermission.build());
    }
}

