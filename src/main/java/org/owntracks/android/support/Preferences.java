package org.owntracks.android.support;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.R;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TimeUtils;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;

import org.json.JSONException;

import de.greenrobot.dao.query.Query;
import de.greenrobot.event.EventBus;

import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.services.ServiceProxy;

public class Preferences {
    private static String subTopicFallback;

    public static SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(App.getContext());
    }

    public static String getKey(int resId) {
        return App.getContext().getString(resId);
    }

    public static boolean getBoolean(int resId, int defId) {
        return getSharedPreferences().getBoolean(getKey(resId), getBooleanRessource(defId));
    }

    public static boolean getBooleanRessource(int resId) {
        return App.getContext().getResources().getBoolean(resId);
    }

    public static int getInt(int resId, int defId) {
        return getSharedPreferences().getInt(getKey(resId), getIntResource(defId));
    }

    public static int getIntResource(int resId) {
        return App.getContext().getResources().getInteger(resId);
    }

    public static String getString(int resId, int defId) {
        String s = getSharedPreferences().getString(getKey(resId), getStringRessource(defId));
        if ((s == null) || s.equals(""))
            s = getStringRessource(defId);

        return s;
    }

    public static String getStringRessource(int resId) {
        return App.getContext().getResources().getString(resId);
    }


    public static void setString(int resId, String value) {
        getSharedPreferences().edit().putString(getKey(resId), value).commit();
    }

    public static void setInt(int resId, int value) {
        getSharedPreferences().edit().putInt(getKey(resId), value).commit();
    }

    public static void setBoolean(int resId, boolean value) {
        getSharedPreferences().edit().putBoolean(getKey(resId), value).commit();
    }

    public static void clearKey(int resId) {
        getSharedPreferences().edit().remove(getKey(resId)).commit();
    }


    public static String getAndroidId() {
        return App.getAndroidId();
    }

    public static boolean canConnect() {

        return  !getHost(false).trim().equals("")
                && ((getAuth() && !getUsername().trim().equals("") && !getPassword().trim().equals("")) || (!getAuth()))
                ;
    }

    public static JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("_type", "configuration")
                    .put(getStringRessource(R.string.keyDeviceId), getDeviceId(true))
                    .put(getStringRessource(R.string.keyClientId), getClientId(true))
                    .put(getStringRessource(R.string.keyHost), getHost())
                    .put(getStringRessource(R.string.keyPort), getPort())
                    .put(getStringRessource(R.string.keyPassword), getPassword())
                    .put(getStringRessource(R.string.keyUsername), getUsername())
                    .put(getStringRessource(R.string.keyPubQos), getPubQos())
                    .put(getStringRessource(R.string.keyKeepalive), getKeepalive())
                    .put(getStringRessource(R.string.keyPubRetain), getPubRetain())
                    .put(getStringRessource(R.string.keyTls), getTls())
                    .put(getStringRessource(R.string.keyTlsCrtPath), getTlsCrtPath())
                    .put(getStringRessource(R.string.keyLocatorDisplacement), getLocatorDisplacement())
                    .put(getStringRessource(R.string.keyLocatorInterval), getLocatorInterval())
                    .put(getStringRessource(R.string.keyAuth), getAuth())
                    .put(getStringRessource(R.string.keyPubIncludeBattery), getPubIncludeBattery())
                    .put(getStringRessource(R.string.keyConnectionAdvancedMode), getConnectionAdvancedMode())
                    .put(getStringRessource(R.string.keySub), getSub())
                    .put(getStringRessource(R.string.keyPub), getPub())
                    .put(getStringRessource(R.string.keyPubInterval), getPubInterval())
                    .put(getStringRessource(R.string.keyPubTopicBase), getPubTopicBase(true))
                    .put(getStringRessource(R.string.keyNotification), getNotification())
                    .put(getStringRessource(R.string.keyNotificationGeocoder), getNotificationGeocoder())
                    .put(getStringRessource(R.string.keyNotificationLocation), getNotificationLocation())
                    .put(getStringRessource(R.string.keyNotificationTickerOnPublish), getNotificationTickerOnPublish())
                    .put(getStringRessource(R.string.keyNotificationTickerOnWaypointTransition), getNotificationTickerOnWaypointTransition())
                    .put(getStringRessource(R.string.keySubTopic), getSubTopic(true))
                    .put(getStringRessource(R.string.keyAutostartOnBoot), getAutostartOnBoot())
                    .put(getStringRessource(R.string.keyLocatorAccuracyBackground), getLocatorAccuracyBackground())
                    .put(getStringRessource(R.string.keyLocatorAccuracyForeground), getLocatorAccuracyForeground())
                    .put(getStringRessource(R.string.keyBeaconBackgroundScanPeriod), getBeaconBackgroundScanPeriod())
                    .put(getStringRessource(R.string.keyBeaconForegroundScanPeriod), getBeaconForegroundScanPeriod())
                    .put(getStringRessource(R.string.keyRemoteCommandDump), getRemoteCommandDump())
                    .put(getStringRessource(R.string.keyRemoteCommandReportLocation), getRemoteCommandReportLocation())
                    .put(getStringRessource(R.string.keyRemoteConfiguration), getRemoteConfiguration())
                    .put(getStringRessource(R.string.keyCleanSession), getCleanSession())
                    .put(getStringRessource(R.string.keyTrackerId), getTrackerId(true));

            Log.v("Preferences", "toJsonObject: " + json.toString());

        } catch (JSONException e) {
            Log.e("Preferences", e.toString());
        }
        return json;
    }

    public static void fromJsonObject(JSONObject json) {
        if (!isPropperMessageType(json, "configuration"))
            return;

        Log.v("Preferences", "fromJsonObject: " +  json.toString());


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
        try { setTlsCrtPath(json.getString(getStringRessource(R.string.keyTlsCrtPath))); } catch (JSONException e) {}
        try { setLocatorDisplacement(json.getInt(getStringRessource(R.string.keyLocatorDisplacement))); } catch (JSONException e) {}
        try { setLocatorInterval(json.getInt(getStringRessource(R.string.keyLocatorInterval))); } catch (JSONException e) {}
        try { setAuth(json.getBoolean(getStringRessource(R.string.keyAuth))); } catch (JSONException e) {}
        try { setPubIncludeBattery(json.getBoolean(getStringRessource(R.string.keyPubIncludeBattery))); } catch (JSONException e) {}
        try { setConnectionAdvancedMode(json.getBoolean(getStringRessource(R.string.keyConnectionAdvancedMode))); } catch (JSONException e) {}
        try { setSub(json.getBoolean(getStringRessource(R.string.keySub))); } catch (JSONException e) {}
        try { setPub(json.getBoolean(getStringRessource(R.string.keyPub))); } catch (JSONException e) {}
        try { setPubInterval(json.getInt(getStringRessource(R.string.keyPubInterval))); } catch (JSONException e) {}
        try { setPubTopicBase(json.getString(getStringRessource(R.string.keyPubTopicBase))); } catch (JSONException e) {}
        try { setNotification(json.getBoolean(getStringRessource(R.string.keyNotification))); } catch (JSONException e) {}
        try { setNotificationGeocoder(json.getBoolean(getStringRessource(R.string.keyNotificationGeocoder))); } catch (JSONException e) {}
        try { setNotificationLocation(json.getBoolean(getStringRessource(R.string.keyNotificationLocation))); } catch (JSONException e) {}
        try { setNotificationTickerOnPublish(json.getBoolean(getStringRessource(R.string.keyNotificationTickerOnPublish))); } catch (JSONException e) {}
        try { setNotificationTickerOnWaypointTransition(json.getBoolean(getStringRessource(R.string.keyNotificationTickerOnWaypointTransition))); } catch (JSONException e) {}
        try { setSubTopic(json.getString(getStringRessource(R.string.keySubTopic))); } catch (JSONException e) {}
        try { setAutostartOnBoot(json.getBoolean(getStringRessource(R.string.keyAutostartOnBoot))); } catch (JSONException e) {}
        try { setLocatorAccuracyBackground(json.getInt(getStringRessource(R.string.keyLocatorAccuracyBackground))); } catch (JSONException e) {}
        try { setLocatorAccuracyForeground(json.getInt(getStringRessource(R.string.keyLocatorAccuracyForeground))); } catch (JSONException e) {}
        try { setRemoteCommandDump(json.getBoolean(getStringRessource(R.string.keyRemoteCommandDump))); } catch (JSONException e) {}
        try { setRemoteCommandReportLocation(json.getBoolean(getStringRessource(R.string.keyRemoteCommandReportLocation))); } catch (JSONException e) {}
        try { setRemoteConfiguration(json.getBoolean(getStringRessource(R.string.keyRemoteConfiguration))); } catch (JSONException e) {}
        try { setCleanSession(json.getBoolean(getStringRessource(R.string.keyCleanSession))); } catch (JSONException e) {}
        try { setTrackerId(json.getString(getStringRessource(R.string.keyTrackerId))); } catch (JSONException e) {}   // TO BE TESTED
        try { setBeaconBackgroundScanPeriod(json.getInt(getStringRessource(R.string.keyBeaconBackgroundScanPeriod))); } catch (JSONException e) {}
        try { setBeaconForegroundScanPeriod(json.getInt(getStringRessource(R.string.keyBeaconForegroundScanPeriod))); } catch (JSONException e) {}
        try {
            JSONArray j = json.getJSONArray("waypoints");
            if (j != null) {
                waypointsFromJson(j);
            } else {
                Log.v("import", "no valid waypoints");
            }
        } catch(JSONException e){
            Log.v("import", "waypoints invalid with exception: " + e);

        };
    }

    private static void waypointsFromJson(JSONArray j) {
        Log.v("import", "importing " + j.length()+" waypoints");
        WaypointDao dao = App.getWaypointDao();
        List<Waypoint> deviceWaypoints =  dao.loadAll();

        for(int i = 0 ; i < j.length(); i++){
            Log.v("import", "importing waypoint: " + i);
            Waypoint newWaypoint;
            JSONObject waypointJson;
            try {
                Log.v("import", "checking for required attributes");

                waypointJson = j.getJSONObject(i);
                newWaypoint  = new Waypoint();
                newWaypoint.setLatitude(waypointJson.getDouble("lat"));
                newWaypoint.setLongitude(waypointJson.getDouble("lon"));
                newWaypoint.setDescription(waypointJson.getString("desc"));

            } catch (JSONException e) {
                // If the above fails we're missing a required attribute and cannot continue the import
                Log.v("import", "missing essential waypoint data: " + e);

                continue;
            }

            try {
                newWaypoint.setShared(waypointJson.getBoolean("shared"));
            } catch (JSONException e) {
                Log.v("import", "unable to import shared attribute");
                newWaypoint.setShared(false);
            }

            if(newWaypoint.getShared()) {
                try {
                    newWaypoint.setRadius(waypointJson.getInt("rad"));
                    int transition = waypointJson.getInt("transition");
                    if(transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT || transition == (Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT))
                        newWaypoint.setTransitionType(transition);
                } catch(Exception e) {
                    Log.v("import", "unable to import radius and/or transition attribute");

                    newWaypoint.setRadius(0);
                }
            }

            try {
                newWaypoint.setDate(new java.util.Date(waypointJson.getLong("tst") * 1000));
            } catch(JSONException e) {
                Log.v("import", "unable to import date attribute");

                newWaypoint.setDate(new Date());
            }


            Waypoint existingWaypoint = null;
            for(Waypoint e : deviceWaypoints) {
                Log.v("import", "existing waypoint tst: " + TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()));
                Log.v("import", "new waypoint tst     : " + TimeUnit.MILLISECONDS.toSeconds(newWaypoint.getDate().getTime()));

                if(TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()) == TimeUnit.MILLISECONDS.toSeconds(newWaypoint.getDate().getTime())) {
                    existingWaypoint = e;
                    break;
                }
            }
            if(existingWaypoint != null) {
                Log.v("Preferences", "found existing waypoint with same date. Deleting it before import");
                dao.delete(existingWaypoint);
                EventBus.getDefault().post(new Events.WaypointRemoved(existingWaypoint));
            } else {
                Log.v("Preferences", "waypoint does not exist, doing clean import");
            }

            dao.insert(newWaypoint);
            EventBus.getDefault().post(new Events.WaypointAdded(newWaypoint));


        }

    }

    public static boolean getRemoteConfiguration() {
        return getBoolean(R.string.keyRemoteConfiguration, R.bool.valRemoteConfiguration);
    }

    public static boolean getRemoteCommandReportLocation() {
        return getBoolean(R.string.keyRemoteCommandReportLocation, R.bool.valRemoteCommandReportLocation);
    }

    public static boolean getRemoteCommandDump() {
        return getBoolean(R.string.keyRemoteCommandDump, R.bool.valRemoteCommandDump);
    }

    public static void setRemoteConfiguration(boolean aBoolean) {
        setBoolean(R.string.keyRemoteConfiguration, aBoolean);
    }

    public static void setRemoteCommandReportLocation(boolean aBoolean) {
        setBoolean(R.string.keyRemoteCommandReportLocation, aBoolean);
    }

    public static void setRemoteCommandDump(boolean aBoolean) {
        setBoolean(R.string.keyRemoteCommandDump, aBoolean);
    }

    public static void setCleanSession(boolean aBoolean) {
        setBoolean(R.string.keyCleanSession, aBoolean);
    }
    public static boolean getCleanSession() {
        return getBoolean(R.string.keyCleanSession,R.bool.valCleanSession);
    }

    public static boolean getConnectionAdvancedMode() {
        return getBoolean(R.string.keyConnectionAdvancedMode, R.bool.valConnectionAdvancedMode);
    }

    public static boolean getPubIncludeBattery() {
        return getBoolean(R.string.keyPubIncludeBattery,
                R.bool.valPubIncludeBattery);
    }

    public static boolean getSub() {
        return getBoolean(R.string.keySub, R.bool.valSub);
    }

    public static boolean getFollowingSelectedContact() {
        return getBoolean(R.string.keyFollowingSelectedContact, R.bool.valFalse);
    }

    public static void setFollowingSelectedContact(boolean following) {
        Log.v("Preferences", "foolow mode for selected contact: " + following);
        setBoolean(R.string.keyFollowingSelectedContact, following);
    }

    public static String getSelectedContactTopic() {
        return getString(R.string.keySelectedContactTopic, R.string.valEmpty);
    }

    public static void setSelectedContactTopic(String topic) {
        setString(R.string.keySelectedContactTopic, topic);
    }

    public static int getLocatorDisplacement() {
        return getInt(R.string.keyLocatorDisplacement, R.integer.valLocatorDisplacement);
    }

    public static long getLocatorIntervalMillis() {
        return TimeUnit.MINUTES.toMillis(getInt(
                R.string.keyLocatorInterval,
                R.integer.valLocatorInterval));
    }

    // Locator interval is set by the user in minutes and therefore should be exported/imported in minutes.
    // getLocatorIntervalMillis can be used to get the millisec value (e.g as needed by ServiceLocator)
    public static int getLocatorInterval() {
        return getInt(R.string.keyLocatorInterval, R.integer.valLocatorInterval);
    }

    public static String getUsername() {
        return getString(R.string.keyUsername, R.string.valEmpty);
    }

    public static boolean getAuth() {
        return getBoolean(R.string.keyAuth, R.bool.valAuth);

    }

    public static String getDeviceId(boolean fallback) {
        String deviceId = getString(R.string.keyDeviceId, R.string.valEmpty);
        if ("".equals(deviceId) && fallback)
            deviceId = getDeviceIdFallback();
        return deviceId;
    }

    public static String getDeviceIdFallback() {
        return getAndroidId();
    }

    public static String getClientId(boolean fallback) {
        String clientId = getString(R.string.keyClientId, R.string.valEmpty);
        if ("".equals(clientId) && fallback)
            clientId = getClientIdFallback();
        return clientId;
    }

    public static String getClientIdFallback() {
        String username = getUsername();
        String deviceId = getDeviceId(true);

        return !"".equals(username) ? username+"/"+deviceId : deviceId;
    }

    public static String getTmpClientIdFallback(String username, String deviceId) {
        String d;
        if(!"".equals(deviceId))
            d = deviceId;
        else
            d = Preferences.getAndroidId();

        return !"".equals(username) ? username+"/"+d : d;
    }

    public static void setClientId(String clientId) {
        setString(R.string.keyClientId, clientId);
    }

    public static String getSubTopic(boolean defaultTopicFallback) {
        String topic = getString(R.string.keySubTopic, R.string.valSubTopic);
        if (topic.equals("") && defaultTopicFallback)
            topic = App.getContext().getString(R.string.valSubTopic);
        return topic;
    }

    public static String getPubTopicBase(boolean fallback) {
        String topic = getString(R.string.keyPubTopicBase, R.string.valEmpty);
        if (topic.equals("") && fallback)
            topic = getPubTopicFallback();

        return topic;
    }

    public static String getBaseTopic() {
        return getPubTopicBase(true);
    }

    public static String getTrackerId(boolean fallback) {

        String tid=getString(R.string.keyTrackerId, R.string.valEmpty);

        if(tid==null || tid.isEmpty())
            return fallback ? getTrackerIdFallback() : "";
        else
            return tid;
    }

    public static String getTrackerIdFallback(){
        String deviceId = getDeviceId(true);

        if(deviceId!=null && deviceId.length() >= 2)
            return deviceId.substring(deviceId.length() - 2);   // defaults to the last two characters of configured topic.
        else
            return "na";  // Empty trackerId won't be included in the message. Alternatively, "na" not available could be returned?
    }



    public static String getTmpTrackerIdFallback(String deviceId) {
        String d;
        if(!"".equals(deviceId))
            d = deviceId;
        else
            d = Preferences.getAndroidId();

        if(d.length() >= 2)
            return d.substring(d.length() - 2);
        else
            return "na";
    }

    public static String getPubTopicPartWaypoints() {
        return getStringRessource(R.string.valPubTopicPartWaypoints);
    }

    public static String getPubTopicFallback() {
        String deviceId = getDeviceId(true);
        String username = getUsername();

        return deviceId.equals("") || username.equals("") ? "" : String.format(getStringRessource(R.string.valPubTopicBase), username, deviceId);
    }

    public static void setHost(String value) {
        if (!value.equals(getHost())) {
            setString(R.string.keyHost, value);
            brokerChanged();
        }
    }

    public static void setPortDefault(int value) {
        clearKey(R.string.keyPort);
    }

    public static void setPort(int value) {
        if (value != getPort()) {
            setInt(R.string.keyPort, value);
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
        return getInt(R.string.keyPort, R.integer.valPort);
    }


    public static String getIntSupportingHint(int key){
        int i = getInt(key, R.integer.valInvalid);
        if (i == -1) {
            return "";
        } else {
            return Integer.toString(i);
        }
    }

    public static String getPortSupportingHint() {
        return getIntSupportingHint(R.string.keyPort);
    }

    public static void setKeepalive(int value) {
        setInt(R.string.keyKeepalive, value);
    }


    public static String getKeepaliveSupportingHint() {
        return getIntSupportingHint(R.string.keyKeepalive);
    }

    //Seconds between ping messages
    public static int getKeepalive() {
        return getInt(R.string.keyKeepalive, R.integer.valKeepalive);
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

    private static void setConnectionAdvancedMode(boolean aBoolean) {
        setBoolean(R.string.keyConnectionAdvancedMode, aBoolean);

    }

    private static void setSub(boolean aBoolean) {
        setBoolean(R.string.keySub, aBoolean);
    }

    private static void setPub(boolean aBoolean) {
        setBoolean(R.string.keyPub, aBoolean);
    }

    private static void setPubInterval(int anInt) {
        setInt(R.string.keyPubInterval, anInt);

    }

    private static void setPubTopicBase(String string) {
        setString(R.string.keyPubTopicBase, string);
    }

    private static void setNotification(boolean aBoolean) {
        setBoolean(R.string.keyNotification, aBoolean);

    }

    private static void setNotificationLocation(boolean aBoolean) {
        setBoolean(R.string.keyNotificationLocation, aBoolean);

    }

    private static void setNotificationTickerOnPublish(boolean aBoolean) {
        setBoolean(R.string.keyNotificationTickerOnPublish, aBoolean);

    }

    private static void setNotificationTickerOnWaypointTransition(boolean aBoolean) {
        setBoolean(R.string.keyNotificationTickerOnWaypointTransition, aBoolean);
    }

    private static void setSubTopic(String string) {
        setString(R.string.keySubTopic, string);

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

    private static void setNotificationGeocoder(boolean aBoolean) {
        setBoolean(R.string.keyNotificationGeocoder, aBoolean);
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
        setBoolean(R.string.keyPubRetain, aBoolean);

    }

    private static void setPubQos(int anInt) {
        setInt(R.string.keyPubQos, anInt);
    }




    public static void setPassword(String password) {
        if (!password.equals(getPassword())) {
            setString(R.string.keyPassword, password);
            brokerChanged();
        }
    }


    public static void setDeviceId(String deviceId) {
        setString(R.string.keyDeviceId, deviceId);
    }

    public static void setAuth(boolean auth) {
        setBoolean(R.string.keyAuth, auth);
    }

    public static void setTls(boolean tlsSpecifier) {
        setBoolean(R.string.keyTls, tlsSpecifier);
    }

    public static void setTlsCrtPath(String tlsCrtPath) {
        setString(R.string.keyTlsCrtPath, tlsCrtPath);
    }

    private static void brokerChanged() {
        Log.v("Preferences", "broker changed");
        EventBus.getDefault().post(new Events.BrokerChanged());
    }

    public static String getHost(){
        return getHost(true);
    }
    public static String getHost(boolean fallback) {

        String host = getString(R.string.keyHost, R.string.valEmpty);
        if ("".equals(host) && fallback)
            host = getStringRessource(R.string.valHost);
        return host;
    }

    public static String getPassword() {
        return getString(R.string.keyPassword, R.string.valEmpty);
    }

    public static boolean getTls() {


        try {
            return getBoolean(R.string.keyTls, R.bool.valTls);
        } catch (ClassCastException e) { // previous versiones used an int
            int tls = getInt(R.string.keyTls, R.integer.valZero);
            getSharedPreferences().edit().remove(getKey(R.string.keyTls)).commit();
            setTls(tls > 0);
            return tls > 0;
        }
    }

    public static String getTlsCrtPath() {
        return getString(R.string.keyTlsCrtPath, R.string.valEmpty);
    }

    public static boolean getNotification() {
        return getBoolean(R.string.keyNotification,
                R.bool.valNotification);
    }

    public static boolean getNotificationTickerOnWaypointTransition() {
        return getBoolean(R.string.keyNotificationTickerOnWaypointTransition,
                R.bool.valNotificationTickerOnWaypointTransition);
    }

    public static boolean getNotificationTickerOnPublish() {
        return getBoolean(R.string.keyNotificationTickerOnPublish,
                R.bool.valNotificationTickerOnPublish);
    }

    public static boolean getNotificationGeocoder() {
        return getBoolean(R.string.keyNotificationGeocoder,
                R.bool.valNotificationGeocoder);
    }

    public static boolean getNotificationLocation() {
        return getBoolean(R.string.keyNotificationLocation,
                R.bool.valNotificationLocation);
    }

    public static int getPubQos() {
        return getInt(
                R.string.keyPubQos,
                R.integer.valPubQos);
    }

    public static boolean getPubRetain() {
        return getBoolean(R.string.keyPubRetain, R.bool.valPubRetain);
    }

    public static int getPubInterval() {
        return getInt(R.string.keyPubInterval, R.integer.valPubInterval);
    }

    public static boolean getPub() {
        return getBoolean(R.string.keyPub, R.bool.valPub);
    }

    public static boolean getAutostartOnBoot() {
        return getBoolean(R.string.keyAutostartOnBoot,
                R.bool.valAutostartOnBoot);
    }

    public static String getBugsnagApiKey() {
        return App.getContext().getString(R.string.valBugsnagApiKey);
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

    public static int getPortDefault() {
        return getIntResource(R.integer.valPort);
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

    private static int getLocatorAccuracy(int val) {
        switch (val) {
            case 0:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
            case 1:
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            case 2:
                return LocationRequest.PRIORITY_LOW_POWER;
            case 3:
                return LocationRequest.PRIORITY_NO_POWER;
            default:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
        }
    }

    public static String getCustomBeaconLayout() {
        return getString(R.string.keyCustomBeaconLayout, R.string.valEmpty);
    }

    public static boolean getBeaconRangingEnabled() {
        return getBoolean(R.string.keyBeaconRangingEnabled, R.bool.valBeaconRangingEnabled);
    }

    public static Boolean getNotificationOnReceivedWaypointTransition() {
        return getBoolean(R.string.keyNotificationOnReceivedWaypointTransition, R.bool.valNotificationOnReceivedWaypointTransition);
    }

    public static void setNotificationOnReceivedWaypointTransition(boolean val) {
        setBoolean(R.string.keyNotificationOnReceivedWaypointTransition, val);
    }


    // Checks if the app is started for the first time.
    // On every new install this returns true for the first time and false afterwards
    // This has no use yet but may be useful later
    public static boolean handleFirstStart() {
        if(getSharedPreferences().getBoolean(getKey(R.string.keyFistStart), true)) {
            Log.v("Preferences", "Initial appliation launch");
            getSharedPreferences().edit().putBoolean(getKey(R.string.keyFistStart), false).commit();
            return true;
        } else {
            Log.v("Preferences", "Consecutive appliation launch");
            return false;
        }
    }

    public static boolean getNotificationVibrateOnPublish() {
        return getBoolean(R.string.keyNotificationVibrateOnPublish, R.bool.valNotificationVibrateOnPublish);
    }

    public static boolean getNotificationVibrateOnWaypointTransition() {
        return getBoolean(R.string.keyNotificationVibrateOnWaypointTransition, R.bool.valNotificationVibrateOnWayointTransition);
    }


    public static boolean isPropperMessageType(JSONObject json, String type) {
        try {
            if(json == null)
                Log.e("isPropperMessageType", "Atempt to invoke isPropperMessageType on null object");

            if (!json.getString("_type").equals(type))
                throw new JSONException("wrong type");
        } catch (JSONException e) {
            Log.e("isPropperMessageType", "Unable to deserialize " + type  +" object from JSON " + json.toString());
            return false;
        }
        return true;
    }

    public static String getSubTopicFallback() {
        return subTopicFallback;
    }
}
