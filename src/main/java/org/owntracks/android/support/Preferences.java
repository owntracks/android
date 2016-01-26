package org.owntracks.android.support;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
import android.util.Log;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.json.JSONException;

import de.greenrobot.event.EventBus;

import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.WaypointMessage;

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

    private static int modeId = App.MODE_ID_PRIVATE;
    private static String deviceUUID = "";

    public static boolean isModePrivate(){ return modeId == App.MODE_ID_PRIVATE; }

    public static boolean isModeHosted(){ return modeId == App.MODE_ID_HOSTED; }

    public static boolean isModePublic(){ return modeId == App.MODE_ID_PUBLIC; }

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
        deviceUUID = sharedPreferences.getString(getKey(R.string.keyDeviceUUID), "undefined-uuid");
        initMode(sharedPreferences.getInt(getStringRessource(R.string.keyModeId), getIntResource(R.integer.valModeId)));
    }


    public static void initMode(int active) {
        setMode(active, true);
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
            case App.MODE_ID_PRIVATE:
                activeSharedPreferences = privateSharedPreferences;
                break;
            case App.MODE_ID_HOSTED:
                activeSharedPreferences = hostedSharedPreferences;
                break;
            case App.MODE_ID_PUBLIC:
                activeSharedPreferences = publicSharedPreferences;
                break;
        }
        sharedPreferences.edit().putInt(getKey(R.string.keyModeId), modeId).commit();

        // Mode switcher reads from currently active sharedPreferences, so we commit the value to all
        privateSharedPreferences.edit().putInt(getKey(R.string.keyModeId), modeId).commit();
        hostedSharedPreferences.edit().putInt(getKey(R.string.keyModeId), modeId).commit();
        publicSharedPreferences.edit().putInt(getKey(R.string.keyModeId), modeId).commit();

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









    public static String getKey(int resId) {
        return App.getContext().getString(resId);
    }

    public static boolean getBoolean(int resId,  int defId) {
        return getBoolean(resId, defId, defId, defId, false, false);
    }

    public static boolean getBoolean(int resId,  int defIdPrivate, int defIdHosted, int defIdPublic, boolean forceDefIdHosted, boolean forceDefIdPublic) {
        if (isModePublic()) {
            return forceDefIdPublic ? getBooleanRessource(defIdPublic) :  publicSharedPreferences.getBoolean(getKey(resId), getBooleanRessource(defIdPublic));
        } else if(isModeHosted()) {
            return forceDefIdPublic ? getBooleanRessource(defIdHosted) :  hostedSharedPreferences.getBoolean(getKey(resId), getBooleanRessource(defIdHosted));
        }

        return privateSharedPreferences.getBoolean(getKey(resId), getBooleanRessource(defIdPrivate));
    }

    public static boolean getBooleanRessource(int resId) {
        return App.getContext().getResources().getBoolean(resId);
    }

    // For keys that do not need overrides in any modes
    public static int getInt(int resId,  int defId) {
        return getInt(resId, defId, defId, defId, false, false);
    }
    public static int getInt(int resId,  int defIdPrivate, int defIdHosted, int defIdPublic, boolean forceDefIdHosted, boolean forceDefIdPublic) {
        if (isModePublic()) {
            return forceDefIdPublic ? getIntResource(defIdPublic) :  publicSharedPreferences.getInt(getKey(resId), getIntResource(defIdPrivate));
        } else if(isModeHosted()) {
            return forceDefIdHosted ? getIntResource(defIdHosted) :  hostedSharedPreferences.getInt(getKey(resId), getIntResource(defIdHosted));
        }

        return privateSharedPreferences.getInt(getKey(resId), getIntResource(defIdPrivate));
    }
    public static int getIntResource(int resId) {
        return App.getContext().getResources().getInteger(resId);
    }

    // Gets the key from specified preferences
    // If the returned value is an empty string or null, the default id is returned
    // This is a quick fix as an empty string does not return the default value
    private static String getStringWithFallback(SharedPreferences preferences, int resId, int defId) {
        String s = preferences.getString(getKey(resId), "");
        return ("".equals(s)) ? getStringRessource(defId) : s;
    }

    public static String getString(int resId,  int defId) {
        return getString(resId, defId, defId, defId, false, false);
    }
    public static String getString(int resId,  int defIdPrivate, int defIdHosted, int defIdPublic, boolean forceDefIdHosted, boolean forceDefIdPublic) {
        if (isModePublic()) {

            return forceDefIdPublic ? getStringRessource(defIdPublic) : getStringWithFallback(publicSharedPreferences, resId, defIdPublic);
        } else if(isModeHosted()) {

            return forceDefIdHosted ? getStringRessource(defIdHosted) : getStringWithFallback(hostedSharedPreferences, resId, defIdHosted);
        }

        return getStringWithFallback(privateSharedPreferences, resId, defIdPrivate);
    }

    public static String getStringRessource(int resId) {
        return App.getContext().getResources().getString(resId);
    }

    public static void setString(int resId, String value) {
        setString(resId, value, true, true);
    }
    public static void setString(int resId, String value, boolean allowSetWhenHosted, boolean allowSetWhenPublic) {
        if((isModeHosted() && !allowSetWhenHosted || isModePublic() && !allowSetWhenPublic)) {
            Log.e(TAG, "setting of key denied in the current mode: " + getKey(resId));
            return;
        }
        activeSharedPreferences.edit().putString(getKey(resId), value).commit();
    }

    public static void setInt(int resId, int value) {
        setInt(resId, value, true, true);
    }
    public static void setInt(int resId, int value, boolean allowSetWhenHosted, boolean allowSetWhenPublic) {
        if((isModeHosted() && !allowSetWhenHosted || isModePublic() && !allowSetWhenPublic)) {
            Log.e(TAG, "setting of key denied in the current mode: " + getKey(resId));
            return;
        }
        activeSharedPreferences.edit().putInt(getKey(resId), value).commit();
    }
    public static void setBoolean(int resId, boolean value) {
        setBoolean(resId, value, true, true);
    }
    public static void setBoolean(int resId, boolean value, boolean allowSetWhenHosted, boolean allowSetWhenPublic) {
        if((isModeHosted() && !allowSetWhenHosted) || (isModePublic() && !allowSetWhenPublic)) {
            Log.e(TAG, "setting of key denied in the current mode: " + getKey(resId));
            return;
        }
        activeSharedPreferences.edit().putBoolean(getKey(resId), value).commit();
    }

    public static void clearKey(int resId) {
        activeSharedPreferences.edit().remove(getKey(resId)).commit();
    }


    public static int getModeId() { return modeId; }


    public SharedPreferences getActiveSharedPreferences() {
        return activeSharedPreferences;
    }

    public static String getAndroidId() {
        return App.getAndroidId();
    }

    public static boolean canConnect() {
        if(isModePrivate()) {
            return !getHost().trim().equals("") && !getUsername().trim().equals("")  && ((getAuth() &&  !getPassword().trim().equals("")) || !getAuth());
        } else if(isModeHosted()) {
            return !getUsername().trim().equals("") && !getPassword().trim().equals("") && !getDeviceId(false).trim().equals("");
        } else if(isModePublic()) {
            return true;
        }
        return false;
    }



    public static void fromJsonObject(JSONObject json) {
        if (!isPropperMessageType(json, "configuration"))
            return;

        Log.v(TAG, "fromJsonObject: " +  json.toString());

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
        try { setTlsCaCrtPath(json.getString(getStringRessource(R.string.keyTlsCaCrtPath))); } catch (JSONException e) {}
        try { setTlsClientCrtPath(json.getString(getStringRessource(R.string.keyTlsClientCrtPath))); } catch (JSONException e) {}
        try { setTlsClientCrtPassword(json.getString(getStringRessource(R.string.keyTlsClientCrtPassword))); } catch (JSONException e) {}

        try { setLocatorDisplacement(json.getInt(getStringRessource(R.string.keyLocatorDisplacement))); } catch (JSONException e) {}
        try { setLocatorInterval(json.getInt(getStringRessource(R.string.keyLocatorInterval))); } catch (JSONException e) {}
        try { setAuth(json.getBoolean(getStringRessource(R.string.keyAuth))); } catch (JSONException e) {}
        try { setPubIncludeBattery(json.getBoolean(getStringRessource(R.string.keyPubIncludeBattery))); } catch (JSONException e) {}
        try { setPub(json.getBoolean(getStringRessource(R.string.keyPub))); } catch (JSONException e) {}
        try { setDeviceTopicBase(json.getString(getStringRessource(R.string.keyPubTopicBase))); } catch (JSONException e) {}
        try { setNotification(json.getBoolean(getStringRessource(R.string.keyNotification))); } catch (JSONException e) {}
        try { setNotificationLocation(json.getBoolean(getStringRessource(R.string.keyNotificationLocation))); } catch (JSONException e) {}
        try { setNotificationEvents(json.getBoolean(getStringRessource(R.string.keyNotificationEvents))); } catch (JSONException e) {}
        try { setNotificationMessages(json.getBoolean(getStringRessource(R.string.keyNotificationMessages))); } catch (JSONException e) {}

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
        try { setMessaging(json.getBoolean(getStringRessource(R.string.keyMessaging))); } catch (JSONException e) {}
        try { setInfo(json.getBoolean(getStringRessource(R.string.keyInfo))); } catch (JSONException e) {}
        try { setEncryption(json.getBoolean(getStringRessource(R.string.keyEncryption))); } catch (JSONException e) {}
        try { setEncryptionKey(json.getString(getStringRessource(R.string.keyEncryptionKey))); } catch (JSONException e) {}




        try {
            JSONArray j = json.getJSONArray("waypoints");
            if (j != null) {
                waypointsFromJson(j);
            } else {
                Log.v(TAG, "no valid waypoints");
            }
        } catch(JSONException e){
            Log.v(TAG, "waypoints invalid with exception: " + e);

        };
    }

    private static JSONArray waypointsToJSON() {

        JSONArray waypoints = new JSONArray();
        for(Waypoint waypoint : Dao.getWaypointDao().loadAll()) {
            WaypointMessage wpM = new WaypointMessage(waypoint);
            JSONObject wp = wpM.toJSONObject();
            try { wp.put("shared", waypoint.getShared()); } catch (JSONException e) { }
            waypoints.put(wp);
        }
        return waypoints;
    }


    private static void waypointsFromJson(JSONArray j) {
        Log.v(TAG, "importing " + j.length()+" waypoints");
        WaypointDao dao = Dao.getWaypointDao();
        List<Waypoint> deviceWaypoints =  dao.loadAll();

        for(int i = 0 ; i < j.length(); i++){
            Log.v(TAG, "importing waypoint: " + i);
            Waypoint newWaypoint;
            JSONObject waypointJson;
            try {
                Log.v(TAG, "checking for required attributes");

                waypointJson = j.getJSONObject(i);
                newWaypoint  = new Waypoint();
                newWaypoint.setGeofenceLatitude(waypointJson.getDouble("lat"));
                newWaypoint.setGeofenceLongitude(waypointJson.getDouble("lon"));


                String descRaw = waypointJson.getString("desc");
                if (descRaw.contains(":")) {
                    Log.v(TAG, "Beacon payload in waypoint desc attribute is not yet supported");
                    newWaypoint.setDescription(descRaw.split(":")[0]);
                } else if(descRaw.contains("$")) {
                    String[] a = descRaw.split("$");
                    if(a.length == 0) {
                        Log.e(TAG, "Waypoint desc contained a $ sign, indicating a SSID no data was found before the sign");
                        continue;
                    }

                    newWaypoint.setDescription(a[0]);
                    if(a.length > 1)
                        newWaypoint.setWifiSSID(a[1]);
                    else {
                        Log.e(TAG, "Waypoint desc contained a $ sign, indicating a SSID but no data was found after the sign");

                    }
                } else {
                    newWaypoint.setDescription(descRaw);
                }


            } catch (JSONException e) {
                // If the above fails we're missing a required attribute and cannot continue the import
                Log.v(TAG, "missing essential waypoint data: " + e);

                continue;
            }

            try {
                newWaypoint.setShared(waypointJson.getBoolean("shared"));
            } catch (JSONException e) {
                Log.v(TAG, "unable to import shared attribute");
                newWaypoint.setShared(false);
            }


            try {
                newWaypoint.setGeofenceRadius(waypointJson.getInt("rad"));

            } catch(Exception e) {
                Log.v(TAG, "unable to import radius and/or transition attribute");

                newWaypoint.setGeofenceRadius(0);
            }



            try {
                newWaypoint.setDate(new java.util.Date(waypointJson.getLong("tst") * 1000));
            } catch(JSONException e) {
                Log.v(TAG, "unable to import date attribute");

                newWaypoint.setDate(new Date());
            }


            Waypoint existingWaypoint = null;
            for(Waypoint e : deviceWaypoints) {
                Log.v(TAG, "existing waypoint tst: " + TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()));
                Log.v(TAG, "new waypoint tst     : " + TimeUnit.MILLISECONDS.toSeconds(newWaypoint.getDate().getTime()));

                if(TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()) == TimeUnit.MILLISECONDS.toSeconds(newWaypoint.getDate().getTime())) {
                    existingWaypoint = e;
                    break;
                }
            }
            if(existingWaypoint != null) {
                Log.v(TAG, "found existing waypoint with same date. Deleting it before import");
                dao.delete(existingWaypoint);
                EventBus.getDefault().post(new Events.WaypointRemoved(existingWaypoint));
            } else {
                Log.v(TAG, "waypoint does not exist, doing clean import");
            }

            dao.insert(newWaypoint);
            EventBus.getDefault().post(new Events.WaypointAdded(newWaypoint));


        }

    }

    public static boolean getRemoteConfiguration() {
        return getBoolean(R.string.keyRemoteConfiguration, R.bool.valRemoteConfiguration, R.bool.valRemoteConfigurationHosted, R.bool.valRemoteConfigurationPublic, true, true);
    }

    public static boolean getRemoteCommandReportLocation() {
        return getBoolean(R.string.keyRemoteCommandReportLocation, R.bool.valRemoteCommandReportLocation);
    }


    public static void setRemoteConfiguration(boolean aBoolean) {
        setBoolean(R.string.keyRemoteConfiguration, aBoolean, false, false);
    }

    public static void setRemoteCommandReportLocation(boolean aBoolean) {
        setBoolean(R.string.keyRemoteCommandReportLocation, aBoolean, true, true);
    }

    public static void setCleanSession(boolean aBoolean) {
        setBoolean(R.string.keyCleanSession, aBoolean, false, false);
    }
    public static boolean getCleanSession() {
        return getBoolean(R.string.keyCleanSession, R.bool.valCleanSession, R.bool.valCleanSessionHosted, R.bool.valCleanSessionPublic, true, true);
    }


    public static boolean getPubLocationIncludeBattery() {
        return getBoolean(R.string.keyPubIncludeBattery, R.bool.valPubIncludeBattery, R.bool.valPubIncludeBattery, R.bool.valPubIncludeBattery, false, false);
    }

    public static boolean getFollowingSelectedContact() {
        return getBoolean(R.string.keyFollowingSelectedContact, R.bool.valFalse);
    }

    public static void setFollowingSelectedContact(boolean following) {
        setBoolean(R.string.keyFollowingSelectedContact, following);
    }

    public static String getSelectedContactTopic() {
        Log.v(TAG, "get selected " + getString(R.string.keySelectedContactTopic, R.string.valEmpty));

        return getString(R.string.keySelectedContactTopic, R.string.valEmpty);
    }

    public static String getGeohashMsgTopic() {
        return getString(R.string.keyGeohashTopic, R.string.valGeohashMsgTopic, R.string.valGeohashMsgTopicHosted, R.string.valGeohashMsgTopicPublic, true, true);
    }

    public static void setSelectedContactTopic(String topic) {
        Log.v(TAG, "selecting " + topic);
        setString(R.string.keySelectedContactTopic, topic);
    }

    public static int getLocatorDisplacement() {
        return getInt(R.string.keyLocatorDisplacement, R.integer.valLocatorDisplacement);
    }

    public static long getLocatorIntervalMillis() {
        return TimeUnit.SECONDS.toMillis(getInt(R.string.keyLocatorInterval, R.integer.valLocatorInterval));
    }

    // Locator interval is set by the user in minutes and therefore should be exported/imported in minutes.
    // getLocatorIntervalMillis can be used to get the millisec value (e.g as needed by ServiceLocator)
    public static int getLocatorInterval() {
        return getInt(R.string.keyLocatorInterval, R.integer.valLocatorInterval);
    }

    public static String getUsername() {
        // in public, the username is just used to build the topic public/user/$deviceId
        return getString(R.string.keyUsername, R.string.valEmpty, R.string.valEmpty, R.string.valUsernamePublic, false, true);
    }

    public static boolean getAuth() {

        return getBoolean(R.string.keyAuth, R.bool.valAuth, R.bool.valAuthHosted, R.bool.valAuthPublic, true, true);

    }

    public static String getDeviceId(boolean fallbackToDefault) {
        if(Preferences.isModePublic())
            return getDeviceUUID();

        String deviceId = getString(R.string.keyDeviceId, R.string.valEmpty);
        if ("".equals(deviceId) && fallbackToDefault)
            return getDeviceIdDefault();
        return deviceId;
    }

    // Not used on public, as many people might use the same device type
    public static String getDeviceIdDefault() {
        // Use device name (Mako, Surnia, etc. and strip all non alpha digits)
        return android.os.Build.DEVICE.replace(" ", "-").replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
    }

    public static String getClientId(boolean fallbackToDefault) {
        if(isModePublic())
            return MqttAsyncClient.generateClientId();

        String clientId = getString(R.string.keyClientId, R.string.valEmpty);
        if ("".equals(clientId) && fallbackToDefault)
            clientId = getClientIdDefault();
        return clientId;
    }

    public static String getClientIdDefault() {
        String clientID=getUsername()+"/"+getDeviceId(true);
        return clientID.replaceAll("[^a-zA-Z0-9/]+", "").toLowerCase();
    }

    public static void setClientId(String clientId) {
        setString(R.string.keyClientId, clientId);
    }

    public static void setDeviceTopicBase(String deviceTopic) {
        setString(R.string.keyPubTopicBase, deviceTopic, false, false);
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
    public static String getPubTopicMsgPart() {
        return "/msg";
    }


    public static String getPubTopicBaseDefault() {
        String formatString;
        String username;
        String deviceId = getDeviceId(true); // will use App.SESSION_UUID on public



        if(isModeHosted()) {
            username = getUsername();
            formatString = getStringRessource(R.string.valPubTopicHosted);
        } else if(isModePublic()) {
            username = "user";
            formatString = getStringRessource(R.string.valPubTopicPublic);
        } else {
            username = getUsername();
            formatString = getStringRessource(R.string.valPubTopic);
        }

        return String.format(formatString, username, deviceId);
    }

    public static String getPubTopicBase(boolean fallbackToDefault) {
        if(!isModePrivate()) {
            return getPubTopicBaseDefault();
        }
        String topic = getString(R.string.keyPubTopicBase, R.string.valEmpty);
        if (topic.equals("") && fallbackToDefault)
            topic = getPubTopicBaseDefault();

        return topic;
    }


    public static String getSubTopic() {
        return getString(R.string.keySubTopic, R.string.valSubTopic, R.string.valSubTopicHosted, R.string.valSubTopicPublic, true, true);
    }

    public static String getTrackerId(boolean fallback) {

        String tid = getString(R.string.keyTrackerId, R.string.valEmpty);

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
        if (!value.equals(getHost())) {
            setString(R.string.keyHost, value, false, false);
            brokerChanged();
        }
    }

    public static void setPortDefault(int value) {
        clearKey(R.string.keyPort);
    }

    public static void setPort(int value) {
        if (value != getPort()) {
            setInt(R.string.keyPort, value, false, false);
            brokerChanged();
        }
    }

    public static void setTrackerId(String value){
        int len=value.length();
        // value validation - must be max 2 characters, only letters and digits
        if(len>=2){
            value=value.substring(0,2);
            if( Character.isLetterOrDigit(value.charAt(0)) && Character.isLetterOrDigit(value.charAt(1)) )
                setString(R.string.keyTrackerId, value);
        }
        else {
            if( len >0 && Character.isLetterOrDigit(value.charAt(0)))
                setString(R.string.keyTrackerId, value);
            else
                setString(R.string.keyTrackerId,"");
        }

    }


    public static int getPort() {
        return getInt(R.string.keyPort, R.integer.valPort, R.integer.valPortHosted, R.integer.valPortPublic, true, true);
    }


    public static String getIntWithHintSupport(int key) {
        int i = getInt(key, R.integer.valInvalid);
        if (i == -1) {
            return "";
        } else {
            return Integer.toString(i);
        }
    }

    public static String getPortWithHintSupport() {
        return getIntWithHintSupport(R.string.keyPort);
    }

    public static void setKeepalive(int value) {
        setInt(R.string.keyKeepalive, value, false, false);
    }


    public static String getKeepaliveWithHintSupport() {
        return getIntWithHintSupport(R.string.keyKeepalive);
    }

    public static int getKeepalive() {
        return getInt(R.string.keyKeepalive, R.integer.valKeepalive, R.integer.valKeepaliveHosted, R.integer.valKeepalivePublic, true, true);
    }

    public static void setUsername(String value) {
        if (!value.equals(getUsername())) {

            setString(R.string.keyUsername, value);
            brokerChanged();
        }
    }


    private static void setPubIncludeBattery(boolean aBoolean) {
        setBoolean(R.string.keyPubIncludeBattery, aBoolean);
    }


    private static void setPub(boolean aBoolean) {
        setBoolean(R.string.keyPub, aBoolean);
    }

    private static void setNotification(boolean aBoolean) {
        setBoolean(R.string.keyNotification, aBoolean);

    }

    private static void setNotificationLocation(boolean aBoolean) {
        setBoolean(R.string.keyNotificationLocation, aBoolean);
    }

    public static void setNotificationEvents(boolean notificationEvents) {
        setBoolean(R.string.keyNotificationEvents, notificationEvents);
    }

    public static void setNotificationMessages(boolean notificationMessages) {
        setBoolean(R.string.keyNotificationMessages, notificationMessages);
    }



    private static void setSubTopic(String string) {
        setString(R.string.keySubTopic, string, false, false);
    }

    private static void setAutostartOnBoot(boolean aBoolean) {
        setBoolean(R.string.keyAutostartOnBoot, aBoolean);

    }

    private static void setLocatorAccuracyForeground(int anInt) {
        setInt(R.string.keyLocatorAccuracyForeground, anInt);

    }

    private static void setLocatorAccuracyBackground(int anInt) {
        setInt(R.string.keyLocatorAccuracyBackground, anInt);

    }

    private static void setLocatorInterval(int anInt) {
        setInt(R.string.keyLocatorInterval, anInt);

    }

    private static void setBeaconBackgroundScanPeriod(int anInt) {
        setInt(R.string.keyBeaconBackgroundScanPeriod, anInt);

    }

    private static void setBeaconForegroundScanPeriod(int anInt) {
        setInt(R.string.keyBeaconBackgroundScanPeriod, anInt);

    }

    private static void setLocatorDisplacement(int anInt) {
        setInt(R.string.keyLocatorDisplacement, anInt);

    }

    private static void setPubRetain(boolean aBoolean) {
        setBoolean(R.string.keyPubRetain, aBoolean, false, false);

    }

    private static void setPubQos(int anInt) {
        setInt(R.string.keyPubQos, anInt, false, false);
    }




    public static void setPassword(String password) {
        if (!password.equals(getPassword())) {
            setString(R.string.keyPassword, password);
            brokerChanged();
        }
    }


    public static void setDeviceId(String deviceId) {
        setString(R.string.keyDeviceId, deviceId, true, false);
    }

    public static void setAuth(boolean auth) {
        setBoolean(R.string.keyAuth, auth, false, false);
    }

    public static void setTls(boolean tlsSpecifier) {
        setBoolean(R.string.keyTls, tlsSpecifier, false, false);
    }

    public static void setTlsCaCrtPath(String tlsCrtPath) {
        setString(R.string.keyTlsCaCrtPath, tlsCrtPath, false, false);
    }
    public static void setTlsClientCrtPath(String tlsCrtPath) {
        setString(R.string.keyTlsClientCrtPath, tlsCrtPath, false, false);
    }


    private static void brokerChanged() {
        Log.v(TAG, "broker changed");
        EventBus.getDefault().post(new Events.BrokerChanged());
    }


    public static String getHost() {
        return getString(R.string.keyHost, R.string.valEmpty, R.string.valHostHosted, R.string.valHostPublic, true, true);
    }

    public static String getPassword() {
        return getString(R.string.keyPassword, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty, false, true);
    }

    public static boolean getTls() {
        return getBoolean(R.string.keyTls, R.bool.valTls, R.bool.valTlsHosted, R.bool.valTlsPublic, true, true);
    }

    public static String getTlsCaCrtPath() {
        return getString(R.string.keyTlsCaCrtPath, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty, true, true);
    }

    public static String getTlsClientCrtPath() {
        return getString(R.string.keyTlsClientCrtPath, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty, true, true);
    }


    public static boolean getNotification() {
        return getBoolean(R.string.keyNotification, R.bool.valNotification);
    }

    public static boolean getNotificationLocation() {
        return getBoolean(R.string.keyNotificationLocation, R.bool.valNotificationLocation);
    }

    public static boolean getNotificationMessages() {
        return getBoolean(R.string.keyNotificationMessages, R.bool.valNotificationMessages);
    }
    public static boolean getNotificationEvents() {
        return getBoolean(R.string.keyNotificationEvents, R.bool.valNotificationEvents);
    }


    public static int getPubQos() {
        return getInt(R.string.keyPubQos, R.integer.valPubQos, R.integer.valPubQosHosted, R.integer.valPubQosPublic, false, true);
    }

    public static boolean getPubRetain() {
        return getBoolean(R.string.keyPubRetain, R.bool.valPubRetain, R.bool.valPubRetainHosted, R.bool.valPubRetainPublic, false, true);
    }

    public static boolean getPub() {
        return getBoolean(R.string.keyPub, R.bool.valPub);
    }

    public static boolean getAutostartOnBoot() {
        return getBoolean(R.string.keyAutostartOnBoot, R.bool.valAutostartOnBoot);
    }

    public static String getRepoUrl() {
        return App.getContext().getString(R.string.valRepoUrl);

    }

    public static String getIssuesMail() {
        return App.getContext().getString(R.string.valIssuesMail);

    }

    public static String getTwitterUrl() {
        return App.getContext().getString(R.string.valTwitterUrl);

    }

    public static int getLocatorAccuracyForeground() {
        return getInt(R.string.keyLocatorAccuracyForeground, R.integer.valLocatorAccuracyForeground);
    }

    public static int getBeaconBackgroundScanPeriod() {
        return getInt(R.string.keyBeaconBackgroundScanPeriod, R.integer.valBeaconBackgroundScanPeriod);
    }

    public static int getBeaconForegroundScanPeriod() {
        return getInt(R.string.keyBeaconBackgroundScanPeriod, R.integer.valBeaconForegroundScanPeriod);
    }

    public static int getLocatorAccuracyBackground() {
        return getInt(R.string.keyLocatorAccuracyBackground, R.integer.valLocatorAccuracyBackground);
    }

    public static String getCustomBeaconLayout() {
        return getString(R.string.keyCustomBeaconLayout, R.string.valEmpty);
    }

    public static boolean getBeaconRangingEnabled() {
        return getBoolean(R.string.keyBeaconRangingEnabled, R.bool.valBeaconRangingEnabled);
    }

    public static String getBroadcastMessageTopic() {
        return "/msg";
    }

    public static boolean getBroadcastMessageEnabled() {
        return getMessaging();
    }

    public static boolean getDirectMessageEnable() {
        return getMessaging(); // for now
    }

    public static boolean getMessaging() {
        return getBoolean(R.string.keyMessaging, R.bool.valMessages, R.bool.valMessagesHosted, R.bool.valMessagesPublic, false, false);
    }

    public static boolean getInfo() {
        return getBoolean(R.string.keyInfo, R.bool.valInfo, R.bool.valInfoHosted, R.bool.valInfoPublic, false, false);
    }


    public static void setMessaging(boolean messaging) {
        setBoolean(R.string.keyMessaging, messaging);
    }

    public static void setInfo(boolean info) {
        setBoolean(R.string.keyInfo, info);
    }


    public static String getTlsClientCrtPassword() {
        return getString(R.string.keyTlsClientCrtPassword, R.string.valEmpty);
    }
    public static void setTlsClientCrtPassword(String password) {
        setString(R.string.keyTlsClientCrtPassword, password);
    }


    public static boolean getEncryption() {
        //return getBoolean(R.string.keyEncryption, R.bool.valFalse);
        return true;
    }
    public static void setEncryption(boolean encryption) {
        setBoolean(R.string.keyEncryption, encryption);
    }

    public static String getEncryptionKey() {
        return "strenggeheim";
       // return getString(R.string.keyEncryptionKey, R.string.valEmpty);
    }
    public static void setEncryptionKey(String key) {
        setString(R.string.keyEncryptionKey, key);
    }



    // Checks if the app is started for the first time.
    // On every new install this returns true for the first time and false afterwards
    // This has no use yet but may be useful later
    public static boolean handleFirstStart() {
        if(sharedPreferences.getBoolean(getKey(R.string.keyFistStart), true)) {
            Log.v(TAG, "Initial application launch");
            sharedPreferences.edit().putBoolean(getKey(R.string.keyFistStart), false).commit();
            String uuid = UUID.randomUUID().toString().toUpperCase();
            sharedPreferences.edit().putString(getKey(R.string.keyDeviceUUID), "A"+uuid.substring(1)).commit();
            Answers.getInstance().logCustom(new CustomEvent("App installed"));
            return true;
        } else {
            Log.v(TAG, "Consecutive application launch");
            return false;
        }
    }

    public static boolean isPropperMessageType(JSONObject json, String type) {
        try {
            if(json == null)
                Log.e(TAG, "Attempt to invoke isPropperMessageType on null object");

            if (!json.getString("_type").equals(type))
                throw new JSONException("wrong type");
        } catch (JSONException e) {
            Log.e(TAG, "Unable to deserialize " + type  +" object from JSON " + json.toString());
            return false;
        }
        return true;
    }



    // Maybe make this configurable
    // For now it makes thins easier to change
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

    public static boolean getLocationBasedServicesEnabled() {
        return true;
    }

    public static void setPersistentGeohash (String hash) {
        setString(R.string.keyCurrentGeohash, hash);
    }
    public static String getAndClearPersistentGeohash () {
        String h = getString(R.string.keyCurrentGeohash, R.string.valEmpty);
        clearKey(R.string.keyCurrentGeohash);
        return h;
    }


    public static JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        // Header
        try {json.put("_type", "configuration");} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyModeId), Preferences.getModeId());} catch(JSONException e) {};

        // Misc settings
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorDisplacement), Preferences.getLocatorDisplacement());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorInterval), Preferences.getLocatorInterval());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyPubIncludeBattery), Preferences.getPubLocationIncludeBattery());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyPub), Preferences.getPub());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyNotification), Preferences.getNotification());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyNotificationLocation), Preferences.getNotificationLocation());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyNotificationMessages), Preferences.getNotificationLocation());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyNotificationEvents), Preferences.getNotificationEvents());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyAutostartOnBoot), Preferences.getAutostartOnBoot());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorAccuracyBackground), Preferences.getLocatorAccuracyBackground());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyLocatorAccuracyForeground), Preferences.getLocatorAccuracyForeground());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyBeaconBackgroundScanPeriod), Preferences.getBeaconBackgroundScanPeriod());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyBeaconForegroundScanPeriod), Preferences.getBeaconForegroundScanPeriod());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyRemoteCommandReportLocation), Preferences.getRemoteCommandReportLocation());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyWaypoints), Preferences.waypointsToJSON());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyEncryption), Preferences.getEncryption());} catch(JSONException e) {};
        try {json.put(Preferences.getStringRessource(R.string.keyEncryptionKey), Preferences.getEncryptionKey());} catch(JSONException e) {};

        // Mode specific settings
        switch (getModeId()) {
            case App.MODE_ID_PUBLIC:
                break;

            case App.MODE_ID_HOSTED:
                try {json.put(Preferences.getStringRessource(R.string.keyUsername), Preferences.getUsername());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyPassword), Preferences.getPassword());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyDeviceId), Preferences.getDeviceId(true));} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyClientId), Preferences.getClientId(true));} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyTrackerId), Preferences.getTrackerId(true));} catch(JSONException e) {};

                try {json.put(Preferences.getStringRessource(R.string.keyMessaging), Preferences.getMessaging());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyRemoteConfiguration), Preferences.getRemoteConfiguration());} catch(JSONException e) {};

                break;
            case App.MODE_ID_PRIVATE:
                try {json.put(Preferences.getStringRessource(R.string.keyHost), Preferences.getHost());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyPort), Preferences.getPort());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyAuth), Preferences.getAuth());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyUsername), Preferences.getUsername());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyPassword), Preferences.getPassword());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyDeviceId), Preferences.getDeviceId(true));} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyClientId), Preferences.getClientId(true));} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyTrackerId), Preferences.getTrackerId(true));} catch(JSONException e) {};

                try {json.put(Preferences.getStringRessource(R.string.keyMessaging), Preferences.getMessaging());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyPubQos), Preferences.getPubQos()) ;} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyKeepalive), Preferences.getKeepalive());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyPubRetain), Preferences.getPubRetain());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyTls), Preferences.getTls());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyTlsCaCrtPath), Preferences.getTlsCaCrtPath());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyTlsClientCrtPath), Preferences.getTlsClientCrtPath());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyTlsClientCrtPassword), Preferences.getTlsClientCrtPassword());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyPubTopicBase), Preferences.getPubTopicBase(true));} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keySubTopic), Preferences.getSubTopic());} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyCleanSession), Preferences.getCleanSession());;} catch(JSONException e) {};
                try {json.put(Preferences.getStringRessource(R.string.keyRemoteConfiguration), Preferences.getRemoteConfiguration());} catch(JSONException e) {};

                break;
        }


        return json;
    }
}
