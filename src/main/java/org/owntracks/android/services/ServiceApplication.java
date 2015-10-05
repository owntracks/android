package org.owntracks.android.services;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import org.owntracks.android.activities.ActivityBase;
import org.owntracks.android.activities.ActivityLauncher;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityMessages;
import org.owntracks.android.activities.ActivityPreferencesConnection;
import org.owntracks.android.db.ContactLink;
import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.messages.CardMessage;
import org.owntracks.android.messages.ConfigurationMessage;
import org.owntracks.android.messages.MsgMessage;
import org.owntracks.android.messages.TransitionMessage;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.messages.LocationMessage;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.SnackbarFactory;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.event.EventBus;

public class ServiceApplication implements ProxyableService,
		StaticHandlerInterface {
    private static final String TAG = "ServiceApplication";

    private static boolean playServicesAvailable;
    private ServiceProxy context;
    @Override
    public void onCreate(ServiceProxy context) {
        this.context = context;
        checkPlayServices();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }

    @Override
    public void onEvent(Events.Dummy event) {

    }


    @Override
    public void handleHandlerMessage(Message msg) {
    }

	public static boolean checkPlayServices() {
		playServicesAvailable = ConnectionResult.SUCCESS == GooglePlayServicesUtil.isGooglePlayServicesAvailable(App.getContext());

		//if (!playServicesAvailable)
		//	showPlayServicesNotAvilableNotification();

		return playServicesAvailable;

	}
}
