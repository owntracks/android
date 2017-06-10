package org.owntracks.android.support;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
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
import org.owntracks.android.services.ServiceLocator;
import org.owntracks.android.services.ServiceNotification;
import org.owntracks.android.services.ServiceProxy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class NotificationProvider {
    private static final int NOTIFICATION_ID_ONGOING = 1;

    private static final int NOTIFICATION_ID_EVENTS_GROUP = 2;

    private final SimpleDateFormat dateFormater;
    private final Preferences.OnPreferenceChangedListener preferencesChangedListener;
    private final Resources resources;

    private NotificationCompat.Builder notificationBuilderOngoing;
    private NotificationCompat.Builder notificationBuilderEvents;
    private NotificationCompat.Builder notificationBuilderEventsGroup;
    private NotificationManager mNotificationManager;

    public NotificationProvider(Context context, Resources resources, Locale locale) {
        mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.resources = resources;
        this.dateFormater = new SimpleDateFormat("HH:mm", locale);
        this.preferencesChangedListener = new Preferences.OnPreferenceChangedListener() {
            @Override
            public void onAttachAfterModeChanged() {
                clearNotifications();
                setupNotificationOngoing();
                setupNotificationEvents();
                updateNotifications();
            }

            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if (key.equals(Preferences.Keys.NOTIFICATION) || key.equals(Preferences.Keys.NOTIFICATION_LOCATION) || key.equals(Preferences.Keys.NOTIFICATION_EVENTS)) {
                    Timber.v("notification prefs changed");
                    clearNotifications();
                    setupNotificationOngoing();
                    setupNotificationEvents();
                    updateNotifications();
                }
            }
        };

        Preferences.registerOnPreferenceChangedListener(this.preferencesChangedListener);
        setupNotificationOngoing();
        setupNotificationEvents();
        updateNotifications();

        App.getEventBus().register(this);
    }



    public void updateNotifications() {
        // TODO: stub
    }

    public void clearNotifications() {
        // TODO: stub
    }

    public void setupNotificationOngoing() {
        if (!Preferences.getNotification())
            return;

        notificationBuilderOngoing = new NotificationCompat.Builder(App.getContext());

        Intent resultIntent = new Intent(App.getContext(), ActivityWelcome.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(App.getContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderOngoing.setContentIntent(resultPendingIntent);
        notificationBuilderOngoing.setSortKey("a");

        notificationBuilderOngoing.addAction(R.drawable.ic_report_notification, resources.getString(R.string.publish), ServiceProxy.getPendingIntentForService(App.getContext(), ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL, null, PendingIntent.FLAG_CANCEL_CURRENT));

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            notificationBuilderOngoing.setColor(App.getContext().getResources().getColor(R.color.primary, App.getContext().getTheme()));
            notificationBuilderOngoing.setCategory(NotificationCompat.CATEGORY_SERVICE);
            notificationBuilderOngoing.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        notificationBuilderOngoing.setOngoing(true);
    }

    public void setupNotificationEvents() {
        if (!Preferences.getNotificationEvents())
            return;

        notificationBuilderEvents = new NotificationCompat.Builder(App.getContext());
        notificationBuilderEventsGroup = new NotificationCompat.Builder(App.getContext());

        Intent resultIntent = new Intent(App.getContext(), ActivityWelcome.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(App.getContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderEvents.setContentIntent(resultPendingIntent);
        notificationBuilderEvents.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(App.getContext(), ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEvents.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEvents.setAutoCancel(true);
        notificationBuilderEvents.setShowWhen(true);
        notificationBuilderEvents.setGroup(NOTIFICATION_ID_EVENTS_GROUP + "");

        notificationBuilderEventsGroup.setContentIntent(resultPendingIntent);
        notificationBuilderEventsGroup.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(App.getContext(), ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEventsGroup.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEventsGroup.setAutoCancel(true);
        notificationBuilderEventsGroup.setShowWhen(true);
        notificationBuilderEventsGroup.setGroup(NOTIFICATION_ID_EVENTS_GROUP + "");
        notificationBuilderEventsGroup.setGroupSummary(true);

        notificationBuilderEvents.setColor(ContextCompat.getColor(App.getContext(), R.color.primary));
        notificationBuilderEvents.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationBuilderEvents.setCategory(NotificationCompat.CATEGORY_SERVICE);
        notificationBuilderEvents.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationBuilderEventsGroup.setColor(ContextCompat.getColor(App.getContext(), R.color.primary));
        notificationBuilderEventsGroup.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationBuilderEventsGroup.setCategory(NotificationCompat.CATEGORY_SERVICE);
        notificationBuilderEventsGroup.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    private void clearNotificationEvents() {
        mNotificationManager.cancel(NOTIFICATION_ID_EVENTS_GROUP);
    }


    private void updateNotificationEvents() {
        if(!Preferences.getNotification() || !Preferences.getNotificationEvents())
            clearNotificationEvents();
    }
    public void sendEventNotification(MessageTransition message) {


        if (!Preferences.getNotificationEvents())
            return;

        // Prepare data
        FusedContact c = App.getFusedContact(message.getContactKey());

        String name;
        long when = message.getTst()*1000;
        String dateStr = dateFormater.format(new Date(when));
        String transition =  resources.getString(message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionEntering : R.string.transitionLeaving);
        String location = message.getDesc();

        if (location == null) {
            location = resources.getString(R.string.aLocation);
        }

        if(c != null)
            name = c.getFusedName();
        else {
            name = message.getTid();

            if (name == null) {
                name = message.getContactKey();
            }
        }

        // Add single notification
        notificationBuilderEvents.setContentTitle(name);
        notificationBuilderEvents.setContentText(transition + " " + location);
        notificationBuilderEvents.setWhen(when);
        notificationBuilderEvents.setShowWhen(true);

        // handle building and sending a normal notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        // perform other configuration ...
        builder.setContentTitle();
        // set the group, this is important for later
        builder.setGroup(remoteNotification.getUserNotificationGroup());
        Notification builtNotification = builder.build();

        // deliver the standard notification
        getNotificationManagerService().notify(remoteNotification.getUserNotificationGroup(), remoteNotification.getUserNotificationId(), builtNotification);

        // pass our remote notification through to deliver a stack notification
        sendEventStackNotification(remoteNotification);
    }


    private void sendEventStackNotification(Notification remoteNotification) {
        // only run this code if the device is running 23 or better
        if (Build.VERSION.SDK_INT >= 23) {
            ArrayList<StatusBarNotification> groupedNotifications = new ArrayList<>();

            // step through all the active StatusBarNotifications and
            for (StatusBarNotification sbn : mNotificationManager.getActiveNotifications()) {
                // add any previously sent notifications with a group that matches our RemoteNotification
                // and exclude any previously sent stack notifications
                if (sbn.getId() != NOTIFICATION_ID_EVENTS_GROUP) {
                    groupedNotifications.add(sbn);
                }
            }

            // since we assume the most recent notification was delivered just prior to calling this method,
            // we check that previous notifications in the group include at least 2 notifications
            if (groupedNotifications.size() > 1) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(App.getContext());

                // use convenience methods on our RemoteNotification wrapper to create a title
                builder.setContentTitle(String.format("%s: foo", "new notification title"))
                        .setContentText(String.format("%d new activities", groupedNotifications.size()));

                // for every previously sent notification that met our above requirements,
                // add a new line containing its title to the inbox style notification extender
                NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
                {
                    for (StatusBarNotification activeSbn : groupedNotifications) {
                        String stackNotificationLine = (String)activeSbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE);
                        if (stackNotificationLine != null) {
                            inbox.addLine(stackNotificationLine);
                        }
                    }

                    // the summary text will appear at the bottom of the expanded stack notification
                    // we just display the same thing from above (don't forget to use string
                    // resource formats!)
                    inbox.setSummaryText(String.format("%d new activities", groupedNotifications.size()));
                }
                builder.setStyle(inbox);

                // make sure that our group is set the same as our most recent RemoteNotification
                // and choose to make it the group summary.
                // when this option is set to true, all previously sent/active notifications
                // in the same group will be hidden in favor of the notifcation we are creating
                builder.setGroup(NOTIFICATION_ID_EVENTS_GROUP+"").setGroupSummary(true);

                // if the user taps the notification, it should disappear after firing its content intent
                // and we set the priority to high to avoid Doze from delaying our notifications
                builder.setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH);

                // create a unique PendingIntent using an integer request code.
                final int requestCode = (int)System.currentTimeMillis() / 1000;
                builder.setContentIntent(PendingIntent.getActivity(App.getContext(), requestCode, new Intent(App.getContext(), ActivityWelcome.class), PendingIntent.FLAG_ONE_SHOT));

                Notification stackNotification = builder.build();
                stackNotification.defaults = Notification.DEFAULT_ALL;

                // finally, deliver the notification using the group identifier as the Tag
                // and the TYPE_STACK which will cause any previously sent stack notifications
                // for this group to be updated with the contents of this built summary notification
                mNotificationManager.notify("TAG", NOTIFICATION_ID_EVENTS_GROUP, stackNotification);
            }
        }
    }


    public void processMessage(MessageTransition message) {
        if(message.getRetained())
            return;

        addNotificationEvents(message);

    }

}


