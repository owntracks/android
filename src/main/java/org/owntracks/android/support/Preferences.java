package org.owntracks.android.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.util.ArrayMap;
import android.util.Log;


import org.json.JSONException;

import de.greenrobot.event.EventBus;

import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.messages.MessageWaypoint;

public class Preferences {
    private static final String TAG = "Preferences";

    public static final String FILENAME_PRIVATE = "org.owntracks.android.preferences.private";
    public static final String FILENAME_HOSTED = "org.owntracks.android.preferences.hosted";
    public static final String FILENAME_PUBLIC = "org.owntracks.android.preferences.public";

    private static SharedPreferences activeSharedPreferences;
    private static SharedPreferences sharedPreferences;

    private static SharedPreferences privateSharedPreferences;
    private static SharedPreferences hostedSharedPreferences;
    private static SharedPreferences publicSharedPreferences;

    private static int modeId = App.MODE_ID_MQTT_PRIVATE;
    private static String deviceUUID = "";

    public static boolean isModeMqttPrivate(){ return modeId == App.MODE_ID_MQTT_PRIVATE; }

    public static boolean isModeMqttPublic(){ return modeId == App.MODE_ID_MQTT_PUBLIC; }

    public static String getDeviceUUID() {
        return deviceUUID;
    }

    public static void initialize(Context c){
        Log.v(TAG, "preferences initializing");
        activeSharedPreferencesChangeListener = new LinkedList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c); // only used for modeId and firstStart keys
        privateSharedPreferences = c.getSharedPreferences(FILENAME_PRIVATE, Context.MODE_PRIVATE);
        hostedSharedPreferences = c.getSharedPreferences(FILENAME_HOSTED, Context.MODE_PRIVATE);
        publicSharedPreferences = c.getSharedPreferences(FILENAME_PUBLIC, Context.MODE_PRIVATE);

        handleFirstStart();
        deviceUUID = sharedPreferences.getString(Keys._DEVICE_UUID, "undefined-uuid");
        initMode(sharedPreferences.getInt(Keys.MODE_ID, getIntResource(R.integer.valModeId)));
    }


    public static void initMode(int active) {
        // Check for valid mode IDs and fallback to Private if an invalid mode is set
        if(active == App.MODE_ID_MQTT_PRIVATE || active == App.MODE_ID_MQTT_PUBLIC) {
            setMode(active, true);
        } else {
            setMode(App.MODE_ID_MQTT_PRIVATE, true);
        }
    }

    public static void setMode(int active) {
        setMode(active, false);
    }
    private static void setMode(int active, boolean init){
        Log.v(TAG, "setting mode to: " + active);
        detachAllActivePreferenceChangeListeners();
        int oldModeId = modeId;
        modeId = active;
        switch (modeId) {
            case App.MODE_ID_MQTT_PRIVATE:
                activeSharedPreferences = privateSharedPreferences;
                break;
            case App.MODE_ID_MQTT_PUBLIC:
                activeSharedPreferences = publicSharedPreferences;
                break;
        }
        sharedPreferences.edit().putInt(Keys.MODE_ID, modeId).commit();

        // Mode switcher reads from currently active sharedPreferences, so we commit the value to all
        privateSharedPreferences.edit().putInt(Keys.MODE_ID, modeId).commit();
        hostedSharedPreferences.edit().putInt(Keys.MODE_ID, modeId).commit();
        publicSharedPreferences.edit().putInt(Keys.MODE_ID, modeId).commit();

        attachAllActivePreferenceChangeListeners();

        if(!init) {
            EventBus.getDefault().post(new Events.ModeChanged(oldModeId,modeId));
        }
    }

    private static LinkedList<OnPreferenceChangedListener> activeSharedPreferencesChangeListener;

    public static void registerOnPreferenceChangedListener(OnPreferenceChangedListener listener) {
        activeSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        activeSharedPreferencesChangeListener.push(listener);
    }

    public static void unregisterOnPreferenceChangedListener(OnPreferenceChangedListener listener) {
        activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        activeSharedPreferencesChangeListener.remove(listener);
    }

    private static void detachAllActivePreferenceChangeListeners() {
        for(SharedPreferences.OnSharedPreferenceChangeListener listener : activeSharedPreferencesChangeListener) {
            activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private static void attachAllActivePreferenceChangeListeners() {
        for(OnPreferenceChangedListener listener : activeSharedPreferencesChangeListener) {
            activeSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
            listener.onAttachAfterModeChanged();
        }
    }

    public static String getSupportUrl() {
        return App.getContext().getString(R.string.valCommunityUrl);
    }


    public interface OnPreferenceChangedListener extends SharedPreferences.OnSharedPreferenceChangeListener {
        void onAttachAfterModeChanged();
    }









    //public static String getKey(int resId) {
    //    return App.getContext().getString(resId);
    //}

    public static boolean getBoolean(String key,  int defId) {
        return getBoolean(key, defId, defId, false);
    }

    public static boolean getBoolean(String key, int defIdPrivate, int defIdPublic, boolean forceDefIdPublic) {
        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getBooleanRessource(defIdPublic) :  publicSharedPreferences.getBoolean(key, getBooleanRessource(defIdPublic));
        }

        return privateSharedPreferences.getBoolean(key, getBooleanRessource(defIdPrivate));
    }

    public static boolean getBooleanRessource(int resId) {
        return App.getContext().getResources().getBoolean(resId);
    }

    // For keys that do not need overrides in any modes
    public static int getInt(String key,  int defId) {
        return getInt(key, defId, defId, false);
    }
    public static int getInt(String key,  int defIdPrivate, int defIdPublic, boolean forceDefIdPublic) {
        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getIntResource(defIdPublic) :  publicSharedPreferences.getInt(key, getIntResource(defIdPrivate));
        }

        return privateSharedPreferences.getInt(key, getIntResource(defIdPrivate));
    }
    public static int getIntResource(int resId) {
        return App.getContext().getResources().getInteger(resId);
    }


    public static int getIntegerDefaultValue(int defaultValueResPrivate, int defaultValueResPublic) {
        return isModeMqttPrivate() ? getIntResource(defaultValueResPrivate) : getIntResource(defaultValueResPublic);
    }



    // Gets the key from specified preferences
    // If the returned value is an empty string or null, the default id is returned
    // This is a quick fix as an empty string does not return the default value
    private static String getStringWithFallback(SharedPreferences preferences, String key, int defId) {
        String s = preferences.getString(key, "");
        return ("".equals(s)) ? getStringRessource(defId) : s;
    }

    public static String getString(String key,  int defId) {
        return getString(key, defId, defId, false);
    }
    public static String getString(String key,  int defIdPrivate, int defIdPublic, boolean forceDefIdPublic) {
        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getStringRessource(defIdPublic) : getStringWithFallback(publicSharedPreferences, key, defIdPublic);
        }

        return getStringWithFallback(privateSharedPreferences, key, defIdPrivate);
    }

    public static String getStringDefaultValue(@StringRes  int defIdPrivate, @StringRes int defIdPublic) {
        return isModeMqttPrivate() ? getStringRessource(defIdPrivate) : getStringRessource(defIdPublic);
    }

    public static String getStringRessource(int resId) {
        return App.getContext().getResources().getString(resId);
    }

    public static void setString(String key, String value) {
        setString(key, value, true);
    }
    public static void setString(String key, String value, boolean allowSetWhenPublic) {
        if(isModeMqttPublic() && !allowSetWhenPublic) {
            Log.e(TAG, "setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putString(key, value).commit();
    }

    public static void setInt(String key, int value) {
        setInt(key, value, true);
    }
    public static void setInt(String key, int value, boolean allowSetWhenPublic) {
        if((isModeMqttPublic() && !allowSetWhenPublic)) {
            Log.e(TAG, "setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putInt(key, value).commit();
    }
    public static void setBoolean(String key, boolean value) {
        setBoolean(key, value, true);
    }
    public static void setBoolean(String key, boolean value, boolean allowSetWhenPublic) {
        if(isModeMqttPublic() && !allowSetWhenPublic) {
            Log.e(TAG, "setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putBoolean(key, value).commit();
    }

    public static void clearKey(String key) {
        activeSharedPreferences.edit().remove(key).commit();
    }

    @Export(key =Keys.MODE_ID, exportModeMqttPrivate =true, exportModeMqttPublic =true)
    public static int getModeId() { return modeId; }


    public SharedPreferences getActiveSharedPreferences() {
        return activeSharedPreferences;
    }

    public static String getAndroidId() {
        return App.getAndroidId();
    }

    public static boolean canConnect() {
        if(isModeMqttPrivate()) {
            return !getHost().trim().equals("") && !getUsername().trim().equals("")  && ((getAuth() &&  !getPassword().trim().equals("")) || !getAuth());
        } else if(isModeMqttPublic()) {
            return true;
        }
        return false;
    }







    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface MessageConfigurationProperty {
        public String key();

    }



    public static void importFromMessage(MessageConfiguration m) {

        HashMap<String, Method> methods = getImportMethods();

        for(String key : m.getKeys()) {
            try {
                methods.get(key).invoke(null, m.get(key));
            } catch (IllegalAccessException e)  {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if(m.hasWaypoints()) {
            waypointsFromJson(m.getWaypoints());
        }




/*





        Log.v(TAG, "importFromMessage: " +  json.toString());

        try { setMode(json.getInt(getStringRessource(R.string.keyModeId))); } catch (JSONException e) {}
        try { setDeviceId(json.getString(getStringRessource(R.string.keyDeviceId))); } catch (JSONException e) {}
        try { setClientId(json.getString(getStringRessource(R.string.keyClientId))); } catch (JSONException e) {}
        try { setHost(json.getString(getStringRessource(R.string.keyHost))); } catch (JSONException e) {}
        try { setPort(json.getInt(getStringRessource(R.string.keyPort))); } catch (JSONException e) {}
        try { setPassword(json.getString(getStringRessource(R.string.keyPassword))); } catch (JSONException e) {}
        try { setUsername(json.getString(getStringRessource(R.string.keyUsername))); } catch (JSONException e) {}
        try { setPubQos(json.getInt(getStringRessource(R.string.keyPubQos))); } catch (JSONException e) {}
        try { setKeepalive(json.getInt(getStringRessource(R.string.keyKeepalive))); } catch (JSONException e) {}
        try { setPubRetain(json.getBoolean(getStringRessource(R.string.keyPubRetain))); } catch (JSONException e) {}
        try { setTls(json.getBoolean(getStringRessource(R.string.keyTls))); } catch (JSONException e) {}
        try { setTlsCaCrt(json.getString(getStringRessource(R.string.keyTlsCaCrt))); } catch (JSONException e) {}
        try { setTlsClientCrt(json.getString(getStringRessource(R.string.keyTlsClientCrt))); } catch (JSONException e) {}
        try { setTlsClientCrtPassword(json.getString(getStringRessource(R.string.keyTlsClientCrtPassword))); } catch (JSONException e) {}

        try { setLocatorDisplacement(json.getInt(getStringRessource(R.string.keyLocatorDisplacement))); } catch (JSONException e) {}
        try { setLocatorInterval(json.getInt(getStringRessource(R.string.keyLocatorInterval))); } catch (JSONException e) {}
        try { setAuth(json.getBoolean(getStringRessource(R.string.keyAuth))); } catch (JSONException e) {}
        try { setPubLocationExtendedData(json.getBoolean(getStringRessource(R.string.keyPubIncludeBattery))); } catch (JSONException e) {}
        try { setPub(json.getBoolean(getStringRessource(R.string.keyPub))); } catch (JSONException e) {}
        try { setDeviceTopicBase(json.getString(getStringRessource(R.string.keyPubTopicBase))); } catch (JSONException e) {}
        try { setNotification(json.getBoolean(getStringRessource(R.string.keyNotification))); } catch (JSONException e) {}
        try { setNotificationLocation(json.getBoolean(getStringRessource(R.string.keyNotificationLocation))); } catch (JSONException e) {}
        try { setNotificationEvents(json.getBoolean(getStringRessource(R.string.keyNotificationEvents))); } catch (JSONException e) {}

        try { setSubTopic(json.getString(getStringRessource(R.string.keySubTopic))); } catch (JSONException e) {}
        try { setAutostartOnBoot(json.getBoolean(getStringRessource(R.string.keyAutostartOnBoot))); } catch (JSONException e) {}
        try { setLocatorAccuracyBackground(json.getInt(getStringRessource(R.string.keyLocatorAccuracyBackground))); } catch (JSONException e) {}
        try { setLocatorAccuracyForeground(json.getInt(getStringRessource(R.string.keyLocatorAccuracyForeground))); } catch (JSONException e) {}
        try { setRemoteCommandReportLocation(json.getBoolean(getStringRessource(R.string.keyRemoteCommandReportLocation))); } catch (JSONException e) {}
        try { setRemoteConfiguration(json.getBoolean(getStringRessource(R.string.keyRemoteConfiguration))); } catch (JSONException e) {}
        try { setCleanSession(json.getBoolean(getStringRessource(R.string.keyCleanSession))); } catch (JSONException e) {}
        try { setTrackerId(json.getString(getStringRessource(R.string.keyTrackerId))); } catch (JSONException e) {}   // TO BE TESTED
        try { setBeaconBackgroundScanPeriod(json.getInt(getStringRessource(R.string.keyBeaconBackgroundScanPeriod))); } catch (JSONException e) {}
        try { setBeaconForegroundScanPeriod(json.getInt(getStringRessource(R.string.keyBeaconForegroundScanPeriod))); } catch (JSONException e) {}
        try { setInfo(json.getBoolean(getStringRessource(R.string.keyInfo))); } catch (JSONException e) {}

        try {
            JSONArray j = json.getJSONArray("waypoints");
            if (j != null) {
                //waypointsFromJson(j);
            } else {
                Log.v(TAG, "no valid waypoints");
            }
        } catch(JSONException e){
            Log.v(TAG, "waypoints invalid with exception: " + e);

        }*/
    }

    private static List<MessageWaypoint> waypointsToJSON() {

        List<MessageWaypoint> messages = new LinkedList<>();
        for(Waypoint waypoint : Dao.getWaypointDao().loadAll()) {
            messages.add(MessageWaypoint.fromDaoObject(waypoint));
        }
        return messages;
    }


    private static void waypointsFromJson(List<MessageWaypoint> j) {
        Log.v(TAG, "importing " + j.size()+" waypoints");
        WaypointDao dao = Dao.getWaypointDao();
        List<Waypoint> deviceWaypoints =  dao.loadAll();

        for (MessageWaypoint m: j) {
            Waypoint w = m.toDaoObject();

            for(Waypoint e : deviceWaypoints) {
                Log.v(TAG, "existing waypoint tst: " + TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()));
                Log.v(TAG, "new waypoint tst     : " + TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime()));

                // remove exisiting waypoint before importing new one
                if(TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()) == TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime())) {
                    dao.delete(e);
                    EventBus.getDefault().post(new Events.WaypointRemoved(e));
                }
            }

            dao.insert(w);
            EventBus.getDefault().post(new Events.WaypointAdded(w));
        }
    }

    @Export(key =Keys.REMOTE_CONFIGURATION, exportModeMqttPrivate =true)
    public static boolean getRemoteConfiguration() {
        return getBoolean(Keys.REMOTE_CONFIGURATION, R.bool.valRemoteConfiguration, R.bool.valRemoteConfigurationPublic, true);
    }

    @Export(key =Keys.REMOTE_COMMAND_REPORT_LOCATION, exportModeMqttPrivate =true)
    public static boolean getRemoteCommandReportLocation() {
        return getBoolean(Keys.REMOTE_COMMAND_REPORT_LOCATION, R.bool.valRemoteCommandReportLocation);
    }

    public static void setRemoteConfiguration(boolean aBoolean) {
        setBoolean(Keys.REMOTE_CONFIGURATION, aBoolean, false);
    }

    public static void setRemoteCommandReportLocation(boolean aBoolean) {
        setBoolean(Keys.REMOTE_COMMAND_REPORT_LOCATION, aBoolean, true);
    }

    public static void setCleanSession(boolean aBoolean) {
        setBoolean(Keys.CLEAN_SESSION, aBoolean, false);
    }

    @Export(key =Keys.PUB_EXTENDED_DATA, exportModeMqttPrivate =true)
    public static boolean getCleanSession() {
        return getBoolean(Keys.CLEAN_SESSION, R.bool.valCleanSession,R.bool.valCleanSessionPublic, true);
    }


    @Export(key =Keys.PUB_EXTENDED_DATA, exportModeMqttPrivate =true)
    public static boolean getPubLocationExtendedData() {
        return getBoolean(Keys.PUB_EXTENDED_DATA, R.bool.valPubExtendedData, R.bool.valPubExtendedData, false);
    }

    @Export(key =Keys.LOCATOR_DISPLACEMENT, exportModeMqttPrivate =true, exportModeMqttPublic =true)
    public static int getLocatorDisplacement() {
        return getInt(Keys.LOCATOR_DISPLACEMENT, R.integer.valLocatorDisplacement);
    }

    public static long getLocatorIntervalMillis() {
        return TimeUnit.SECONDS.toMillis(getLocatorInterval());
    }

    @Export(key =Keys.LOCATOR_INTERVAL, exportModeMqttPrivate =true, exportModeMqttPublic =true)
    public static int getLocatorInterval() {
        return getInt(Keys.LOCATOR_INTERVAL, R.integer.valLocatorInterval);
    }

    @Export(key =Keys.USERNAME, exportModeMqttPrivate =true)
    public static String getUsername() {
        // in public, the username is just used to build the topic public/user/$deviceId
        return getString(Keys.USERNAME, R.string.valEmpty, R.string.valUsernamePublic, true);
    }

    @Export(key =Keys.AUTH, exportModeMqttPrivate =true)
    public static boolean getAuth() {
        return getBoolean(Keys.AUTH, R.bool.valAuth, R.bool.valAuthPublic, true);

    }

    @Export(key =Keys.DEVICE_ID, exportModeMqttPrivate =true)
    public static String getDeviceId() {
        return getDeviceId(true);
    }

    public static String getDeviceId(boolean fallbackToDefault) {
        if(Preferences.isModeMqttPublic())
            return getDeviceUUID();

        String deviceId = getString(Keys.DEVICE_ID, R.string.valEmpty);
        if ("".equals(deviceId) && fallbackToDefault)
            return getDeviceIdDefault();
        return deviceId;
    }

    // Not used on public, as many people might use the same device type
    public static String getDeviceIdDefault() {
        // Use device name (Mako, Surnia, etc. and strip all non alpha digits)
        return android.os.Build.DEVICE.replace(" ", "-").replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
    }

    @Export(key =Keys.CLIENT_ID, exportModeMqttPrivate =true)
    public static String getClientId() {
        return getClientId(false);
    }
    public static String getClientId(boolean fallbackToDefault) {
        if(isModeMqttPublic())
            return MqttAsyncClient.generateClientId();

        String clientId = getString(Keys.CLIENT_ID, R.string.valEmpty);
        if ("".equals(clientId) && fallbackToDefault)
            clientId = getClientIdDefault();
        return clientId;
    }

    public static String getClientIdDefault() {
        String clientID=getUsername()+"/"+getDeviceId(true);
        return clientID.replaceAll("[^a-zA-Z0-9/]+", "").toLowerCase();
    }

    public static void setClientId(String clientId) {
        setString(Keys.CLIENT_ID, clientId);
    }

    public static void setDeviceTopicBase(String deviceTopic) {
        setString(Keys.PUB_TOPIC_BASE, deviceTopic, false);
    }

    public static String getPubTopicLocations() {
        return getPubTopicBase(true);
    }

    public static String getPubTopicWaypoints() {
        return getPubTopicBase(true) +getPubTopicWaypointsPart();
    }

    public static String getPubTopicWaypointsPart() {
        return "/waypoint";
    }

    public static String getPubTopicEvents() {
        return getPubTopicBase(true) + getPubTopicEventsPart();
    }

    public static String getPubTopicEventsPart() {
        return "/event";
    }
    public static String getPubTopicInfoPart() {
        return "/info";
    }
    public static String getPubTopicCommands() {
        return getPubTopicBase(true) +getPubTopicCommandsPart();
    }
    public static String getPubTopicCommandsPart() {
        return "/cmd";
    }


    public static String getPubTopicBaseDefault() {
        String formatString;
        String username;
        String deviceId = getDeviceId(true); // will use App.SESSION_UUID on public



        if(isModeMqttPublic()) {
            username = "user";
            formatString = getStringRessource(R.string.valPubTopicPublic);
        } else {
            username = getUsername();
            formatString = getStringRessource(R.string.valPubTopic);
        }

        return String.format(formatString, username, deviceId);
    }

    @Export(key =Keys.PUB_TOPIC_BASE, exportModeMqttPrivate =true)
    public static String getPubTopicBase() {
        return getPubTopicBase(false);
    }
    public static String getPubTopicBase(boolean fallbackToDefault) {
        if(!isModeMqttPrivate()) {
            return getPubTopicBaseDefault();
        }
        String topic = getString(Keys.PUB_TOPIC_BASE, R.string.valEmpty);
        if (topic.equals("") && fallbackToDefault)
            topic = getPubTopicBaseDefault();

        return topic;
    }

    @Export(key =Keys.SUB_TOPIC, exportModeMqttPrivate =true)
    public static String getSubTopic() {
        return getString(Keys.SUB_TOPIC, R.string.valSubTopic, R.string.valSubTopicPublic, true);
    }

    @Export(key =Keys.TRACKER_ID, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static String getTrackerId() {
        return getTrackerId(false);

    }
    public static String getTrackerId(boolean fallback) {

        String tid = getString(Keys.TRACKER_ID, R.string.valEmpty);

        if(tid==null || tid.isEmpty())
            return fallback ? getTrackerIdDefault() : "";
        else
            return tid;
    }

    public static String getTrackerIdDefault(){
        String deviceId = getDeviceId(true);
        if(deviceId!=null && deviceId.length() >= 2)
            return deviceId.substring(deviceId.length() - 2);   // defaults to the last two characters of configured deviceId.
        else
            return "na";  // Empty trackerId won't be included in the message.
    }

    public static void setHost(String value) {
            setString(Keys.HOST, value, false);
    }

    public static void setPortDefault(int value) {
        clearKey(Keys.PORT);
    }

    public static void setPort(int value) {
            setInt(Keys.PORT, value, false);
   }

    public static void setTrackerId(String value){
        int len=value.length();
        // value validation - must be max 2 characters, only letters and digits
        if(len>=2){
            value=value.substring(0,2);
            if( Character.isLetterOrDigit(value.charAt(0)) && Character.isLetterOrDigit(value.charAt(1)) )
                setString(Keys.TRACKER_ID, value);
        }
        else {
            if( len >0 && Character.isLetterOrDigit(value.charAt(0)))
                setString(Keys.TRACKER_ID, value);
            else
                setString(Keys.TRACKER_ID,"");
        }

    }

    @Export(key =Keys.PORT, exportModeMqttPrivate =true)
    public static int getPort() {
        return getInt(Keys.PORT, R.integer.valPort, R.integer.valPortPublic, true);
    }


    public static String getIntWithHintSupport(String key) {
        int i = getInt(key, R.integer.valInvalid);
        if (i == -1) {
            return "";
        } else {
            return Integer.toString(i);
        }
    }

    public static String getPortWithHintSupport() {
        return getIntWithHintSupport(Keys.PORT);
    }

    public static void setKeepalive(int value) {
        setInt(Keys.KEEPALIVE, value, false);
    }


    public static String getKeepaliveWithHintSupport() {
        return getIntWithHintSupport(Keys.KEEPALIVE);
    }

    @Export(key =Keys.KEEPALIVE, exportModeMqttPrivate =true)
    public static int getKeepalive() {
        return getInt(Keys.KEEPALIVE, R.integer.valKeepalive, R.integer.valKeepalivePublic, true);
    }

    public static void setUsername(String value) {

            setString(Keys.USERNAME, value);
    }


    private static void setPubLocationExtendedData(boolean aBoolean) {
        setBoolean(Keys.PUB_EXTENDED_DATA, aBoolean);
    }


    private static void setPub(boolean aBoolean) {
        setBoolean(Keys.PUB, aBoolean);
    }

    private static void setNotification(boolean aBoolean) {
        setBoolean(Keys.NOTIFICATION, aBoolean);

    }

    private static void setNotificationLocation(boolean aBoolean) {
        setBoolean(Keys.NOTIFICATION_LOCATION, aBoolean);
    }

    public static void setNotificationEvents(boolean notificationEvents) {
        setBoolean(Keys.NOTIFICATION_EVENTS, notificationEvents);
    }


    private static void setSubTopic(String string) {
        setString(Keys.SUB_TOPIC, string, false);
    }

    private static void setAutostartOnBoot(boolean aBoolean) {
        setBoolean(Keys.AUTOSTART_ON_BOOT, aBoolean);

    }

    private static void setLocatorAccuracyForeground(int anInt) {
        setInt(Keys.LOCATOR_ACCURACY_FOREGROUND, anInt);

    }

    private static void setLocatorAccuracyBackground(int anInt) {
        setInt(Keys.LOCATOR_ACCURACY_BACKGROUND, anInt);

    }

    private static void setLocatorInterval(int anInt) {
        setInt(Keys.LOCATOR_INTERVAL, anInt);

    }

    private static void setBeaconBackgroundScanPeriod(int anInt) {
        setInt(Keys.BEACON_BACKGROUND_SCAN_PERIOD, anInt);

    }

    private static void setBeaconForegroundScanPeriod(int anInt) {
        setInt(Keys.BEACON_FOREGROUND_SCAN_PERIOD, anInt);

    }

    private static void setLocatorDisplacement(int anInt) {
        setInt(Keys.LOCATOR_DISPLACEMENT, anInt);

    }

    private static void setPubRetain(boolean aBoolean) {
        setBoolean(Keys.PUB_RETAIN, aBoolean, false);

    }

    private static void setPubQos(int anInt) {
        setInt(Keys.PUB_QOS, anInt, false);
    }




    public static void setPassword(String password) {
            setString(Keys.PASSWORD, password);
    }


    public static void setDeviceId(String deviceId) {
        setString(Keys.DEVICE_ID, deviceId, false);
    }

    public static void setAuth(boolean auth) {
        setBoolean(Keys.AUTH, auth, false);
    }

    public static void setTls(boolean tlsSpecifier) {
        setBoolean(Keys.TLS, tlsSpecifier, false);
    }

    public static void setTlsCaCrt(String tlsCrtPath) {
        setString(Keys.TLS_CA_CRT, tlsCrtPath, false);
    }
    public static void setTlsClientCrt(String tlsCrtPath) {
        setString(Keys.TLS_CLIENT_CRT, tlsCrtPath, false);
    }
    @Export(key =Keys.HOST, exportModeMqttPrivate =true)
    public static String getHost() {
        return getString(Keys.HOST, R.string.valEmpty, R.string.valHostPublic, true);
    }
    @Export(key =Keys.PASSWORD, exportModeMqttPrivate =true)
    public static String getPassword() {
        return getString(Keys.PASSWORD, R.string.valEmpty, R.string.valEmpty, true);
    }

    @Export(key =Keys.TLS, exportModeMqttPrivate =true)
    public static boolean getTls() {
        return getBoolean(Keys.TLS, R.bool.valTls, R.bool.valTlsPublic, true);
    }

    @Export(key =Keys.TLS_CA_CRT, exportModeMqttPrivate =true)
    public static String getTlsCaCrtName() {
        return getString(Keys.TLS_CA_CRT, R.string.valEmpty, R.string.valEmpty, true);
    }

    @Export(key =Keys.TLS_CLIENT_CRT, exportModeMqttPrivate =true)
    public static String getTlsClientCrtName() {
        return getString(Keys.TLS_CLIENT_CRT, R.string.valEmpty, R.string.valEmpty, true);
    }

    @Export(key =Keys.NOTIFICATION, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static boolean getNotification() {
        return getBoolean(Keys.NOTIFICATION, R.bool.valNotification);
    }

    @Export(key =Keys.NOTIFICATION_LOCATION, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static boolean getNotificationLocation() {
        return getBoolean(Keys.NOTIFICATION_LOCATION, R.bool.valNotificationLocation);
    }

    public static boolean getNotificationEvents() {
        return getBoolean(Keys.NOTIFICATION_EVENTS, R.bool.valNotificationEvents);
    }

    @Export(key =Keys.PUB_QOS, exportModeMqttPrivate =true)
    public static int getPubQos() {
        return getInt(Keys.PUB_QOS, R.integer.valPubQos, R.integer.valPubQosPublic, true);
    }
    @Export(key =Keys.PUB_RETAIN, exportModeMqttPrivate =true)
    public static boolean getPubRetain() {
        return getBoolean(Keys.PUB_RETAIN, R.bool.valPubRetain, R.bool.valPubRetainPublic, true);
    }

    @Export(key =Keys.PUB, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static boolean getPub() {
        return getBoolean(Keys.PUB, R.bool.valPub);
    }


    @Export(key =Keys.AUTOSTART_ON_BOOT, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static boolean getAutostartOnBoot() {
        return getBoolean(Keys.AUTOSTART_ON_BOOT, R.bool.valAutostartOnBoot);
    }

    public static String getRepoUrl() {
        return App.getContext().getString(R.string.valRepoUrl);

    }

    public static String getTwitterUrl() {
        return App.getContext().getString(R.string.valTwitterUrl);

    }

    @Export(key =Keys.LOCATOR_ACCURACY_FOREGROUND, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static int getLocatorAccuracyForeground() {
        return getInt(Keys.LOCATOR_ACCURACY_FOREGROUND, R.integer.valLocatorAccuracyForeground);
    }

    @Export(key =Keys.BEACON_BACKGROUND_SCAN_PERIOD, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static int getBeaconBackgroundScanPeriod() {
        return getInt(Keys.BEACON_BACKGROUND_SCAN_PERIOD, R.integer.valBeaconBackgroundScanPeriod);
    }

    @Export(key =Keys.BEACON_FOREGROUND_SCAN_PERIOD, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static int getBeaconForegroundScanPeriod() {
        return getInt(Keys.BEACON_FOREGROUND_SCAN_PERIOD, R.integer.valBeaconForegroundScanPeriod);
    }

    @Export(key =Keys.LOCATOR_ACCURACY_BACKGROUND, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static int getLocatorAccuracyBackground() {
        return getInt(Keys.LOCATOR_ACCURACY_BACKGROUND, R.integer.valLocatorAccuracyBackground);
    }

    @Export(key =Keys.BEACON_LAYOUT, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static String getCustomBeaconLayout() {
        return getString(Keys.BEACON_LAYOUT, R.string.valEmpty);
    }
    @Export(key =Keys.BEACON_RANGING, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static boolean getBeaconRangingEnabled() {
        return getBoolean(Keys.BEACON_RANGING, R.bool.valBeaconRangingEnabled);
    }

    // Enable cards
    public static boolean getInfo() {
        return getBoolean(Keys.INFO, R.bool.valInfo, R.bool.valInfoPublic, false);
    }

    public static void setInfo(boolean info) {
        setBoolean(Keys.INFO, info);
    }

    @Export(key =Keys.TLS_CLIENT_CRT_PASSWORD, exportModeMqttPrivate =true)
    public static String getTlsClientCrtPassword() {
        return getString(Keys.TLS_CLIENT_CRT_PASSWORD, R.string.valEmpty);
    }
    public static void setTlsClientCrtPassword(String password) {
        setString(Keys.TLS_CLIENT_CRT_PASSWORD, password);
    }

    @Export(key =Keys._ENCRYPTION_KEY, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static String getEncryptionKey() {
        return getString(Keys._ENCRYPTION_KEY, R.string.valEmpty);
    }

    // Checks if the app is started for the first time.
    // On every new install this returns true for the first time and false afterwards
    // This has no use yet but may be useful later
    public static void handleFirstStart() {
        if(sharedPreferences.getBoolean(Keys._FIST_START, true)) {
            Log.v(TAG, "Initial application launch");
            sharedPreferences.edit().putBoolean(Keys._FIST_START , false).commit();
            String uuid = UUID.randomUUID().toString().toUpperCase();
            sharedPreferences.edit().putString(Keys._DEVICE_UUID, "A"+uuid.substring(1)).commit();
        } else {
            Log.v(TAG, "Consecutive application launch");
        }
    }

    // Maybe make this configurable
    // For now it makes things easier to change
    public static int getPubQosEvents() {
        return getPubQos();
    }

    public static boolean getPubRetainEvents() {
        return false;
    }

    public static int getPubQosCommands() {
        return getPubQos();
    }

    public static boolean getPubRetainCommands() {
        return false;
    }

    public static int getPubQosWaypoints() {
        return 0;
    }

    public static boolean getPubRetainWaypoints() {
        return false;
    }

    public static int getPubQosLocations() {
        return getPubQos();
    }

    public static boolean getPubRetainLocations() {
        return getPubRetain();
    }


    public static MessageConfiguration exportToMessage() {
        List<Method> methods = getExportMethods();
        MessageConfiguration cfg = new MessageConfiguration();
        for(Method m : methods) {
            m.setAccessible(true);

            Log.v(TAG,"method for config key: " + m.getAnnotation(Export.class).key());
            Log.v(TAG,"calling method: " + m.getName());
            Log.v(TAG,"return type: " + m.getReturnType());
            ;

            try {
                //If the underlying method is static, then the specified obj argument is ignored. It may be null.
                cfg.set(m.getAnnotation(Export.class).key(), m.invoke(null, null));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }


        cfg.set("waypoints", waypointsToJSON());

        return cfg;

        /*
        JSONObject json = new JSONObject();

        // Header
        try {json.put("_type", "configuration");} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyModeId), Preferences.getModeId());} catch(JSONException e) {}

        // Misc settings
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorDisplacement), Preferences.getLocatorDisplacement());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorInterval), Preferences.getLocatorInterval());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyPubIncludeBattery), Preferences.getPubLocationExtendedData());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyPub), Preferences.getPub());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyNotification), Preferences.getNotification());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyNotificationLocation), Preferences.getNotificationLocation());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyNotificationEvents), Preferences.getNotificationEvents());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyAutostartOnBoot), Preferences.getAutostartOnBoot());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorAccuracyBackground), Preferences.getLocatorAccuracyBackground());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorAccuracyForeground), Preferences.getLocatorAccuracyForeground());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyBeaconBackgroundScanPeriod), Preferences.getBeaconBackgroundScanPeriod());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyBeaconForegroundScanPeriod), Preferences.getBeaconForegroundScanPeriod());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyRemoteCommandReportLocation), Preferences.getRemoteCommandReportLocation());} catch(JSONException e) {}
        try {json.put(Preferences.getStringRessource(R.string.keyWaypoints), Preferences.waypointsToJSON());} catch(JSONException e) {}

        // Mode specific settings
        switch (getModeId()) {
            case App.MODE_ID_MQTT_PUBLIC:
                break;

            case App.MODE_ID_MQTT_PRIVATE:
                try {json.put(Preferences.getStringRessource(R.string.keyHost), Preferences.getHost());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyPort), Preferences.getPort());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyAuth), Preferences.getAuth());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyUsername), Preferences.getUsername());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyPassword), Preferences.getPassword());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyDeviceId), Preferences.getDeviceId(true));} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyClientId), Preferences.getClientId(true));} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyTrackerId), Preferences.getTrackerId(true));} catch(JSONException e) {}

                try {json.put(Preferences.getStringRessource(R.string.keyPubQos), Preferences.getPubQos()) ;} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyKeepalive), Preferences.getKeepalive());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyPubRetain), Preferences.getPubRetain());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyTls), Preferences.getTls());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyTlsCaCrt), Preferences.getTlsCaCrtName());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyTlsClientCrt), Preferences.getTlsClientCrtName());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyTlsClientCrtPassword), Preferences.getTlsClientCrtPassword());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyPubTopicBase), Preferences.getPubTopicBase(true));} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keySubTopic), Preferences.getSubTopic());} catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyCleanSession), Preferences.getCleanSession());
                } catch(JSONException e) {}
                try {json.put(Preferences.getStringRessource(R.string.keyRemoteConfiguration), Preferences.getRemoteConfiguration());} catch(JSONException e) {}

                break;
        }


        return json;*/
    }

    public static class Keys {
        public static final String AUTH                             = "auth";
        public static final String AUTOSTART_ON_BOOT                = "autostartOnBoot";
        public static final String BEACON_BACKGROUND_SCAN_PERIOD    = "beaconBackgroundScanPeriod";
        public static final String BEACON_FOREGROUND_SCAN_PERIOD    = "beaconForegroundScanPeriod";
        public static final String BEACON_LAYOUT                    = "beaconLayout";
        public static final String BEACON_RANGING                   = "ranging";
        public static final String CLEAN_SESSION                    = "cleanSession";
        public static final String CLIENT_ID                        = "clientId";
        public static final String DEVICE_ID                        = "deviceId";
        public static final String HOST                             = "host";
        public static final String INFO                             = "info";
        public static final String KEEPALIVE                        = "keepalive";
        public static final String LOCATOR_ACCURACY_BACKGROUND      = "locatorAccuracyBackground";
        public static final String LOCATOR_ACCURACY_FOREGROUND      = "locatorAccuracyForeground";
        public static final String LOCATOR_DISPLACEMENT             = "locatorDisplacement";
        public static final String LOCATOR_INTERVAL                 = "locatorInterval";
        public static final String MODE_ID                          = "mode";
        public static final String NOTIFICATION                     = "notification";
        public static final String NOTIFICATION_EVENTS              = "notificationEvents";
        public static final String NOTIFICATION_LOCATION            = "notificationLocation";
        public static final String PASSWORD                         = "password";
        public static final String PORT                             = "port";
        public static final String PUB                              = "pub";
        public static final String PUB_EXTENDED_DATA                = "pubExtendedData";
        public static final String PUB_QOS                          = "pubQos";
        public static final String PUB_RETAIN                       = "pubRetain";
        public static final String PUB_TOPIC_BASE                   = "pubTopicBase";
        public static final String REMOTE_COMMAND_REPORT_LOCATION   = "remoteCommandReportLocation";
        public static final String REMOTE_CONFIGURATION             = "remoteConfiguration";
        public static final String SUB                              = "sub";
        public static final String SUB_TOPIC                        = "subTopic";
        public static final String TLS                              = "tls";
        public static final String TLS_CA_CRT                       = "tlsCaCrt";
        public static final String TLS_CLIENT_CRT                   = "tlsClientCrt";
        public static final String TLS_CLIENT_CRT_PASSWORD          = "tlsClientCrtPassword";
        public static final String TRACKER_ID                       = "trackerId";
        public static final String USERNAME                         = "username";

        // Internal keys
        public static final String _DEVICE_UUID                     = "deviceUUID";
        public static final String _ENCRYPTION_KEY                  = "encryptionKey";
        public static final String _FIST_START                      = "fistStart";

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Export {
        String key();
        boolean exportModeMqttPrivate() default false;
        boolean exportModeMqttPublic() default false;
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Import {
        String key();
        boolean importModeMqttPrivate() default false;
        boolean importModeMqttPublic() default false;
    }

    public static List<Method> getExportMethods() {
        final List<Method> methods = new ArrayList<Method>();
        Class<?> klass  = Preferences.class;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Export.class) ) {
                    Export annotInstance = method.getAnnotation(Export.class);
                    if(getModeId() == App.MODE_ID_MQTT_PRIVATE && annotInstance.exportModeMqttPrivate() || getModeId() == App.MODE_ID_MQTT_PUBLIC && annotInstance.exportModeMqttPublic()) {
                        methods.add(method);
                    }
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    public static HashMap<String, Method> getImportMethods() {
        final HashMap<String, Method> methods = new HashMap<>();
        Class<?> klass  = Preferences.class;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Import.class) ) {
                    Import annotInstance = method.getAnnotation(Import.class);
                    if(getModeId() == App.MODE_ID_MQTT_PRIVATE && annotInstance.importModeMqttPrivate() || getModeId() == App.MODE_ID_MQTT_PUBLIC && annotInstance.importModeMqttPublic()) {
                        methods.put(annotInstance.key(), method);
                    }
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }
}
