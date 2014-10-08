package st.alr.mqttitude.support;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.LocationRequest;

import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.services.ServiceProxy;

public class Preferences {
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

    public static String getAndroidId() {
        return App.getAndroidId();
    }

    public static boolean canConnect() {

        return     !getHost().trim().equals("")
                && ((getAuth() && !getUsername().trim().equals("") && !getPassword().trim().equals("")) || (!getAuth()))
                && ((getTls() == getIntResource(R.integer.valTls)) || (getTls() == getIntResource(R.integer.valTlsNone) || (getTls() == getIntResource(R.integer.valTlsCustom) && !getTlsCrtPath().trim().equals(""))))
                ;
    }

    public static StringifiedJSONObject toJSONObject() {
        StringifiedJSONObject json = new StringifiedJSONObject();
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
                    .put(getStringRessource(R.string.keyUpdateAddressBook), getUpdateAdressBook())
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
                    .put(getStringRessource(R.string.keyRemoteCommandDump), getRemoteCommandDump())
                    .put(getStringRessource(R.string.keyRemoteCommandReportLocation), getRemoteCommandReportLocation())
                    .put(getStringRessource(R.string.keyRemoteConfiguration), getRemoteConfiguration())
                    .put(getStringRessource(R.string.keyCleanSession), getCleanSession());


        } catch (JSONException e) {
            Log.e("Preferences", e.toString());
        }
        return json;
    }

    public static void fromJsonObject(StringifiedJSONObject json) {
        if (!Defaults.isPropperMessageType(json, "configuration"))
            return;


        try { setDeviceId(json.getString(getStringRessource(R.string.keyDeviceId))); } catch (JSONException e) {}
        try { setClientId(json.getString(getStringRessource(R.string.keyClientId))); } catch (JSONException e) {}
        try { setHost(json.getString(getStringRessource(R.string.keyHost))); } catch (JSONException e) {}
        try { setPort(json.getInt(getStringRessource(R.string.keyPort))); } catch (JSONException e) {}
        try { setPassword(json.getString(getStringRessource(R.string.keyPassword))); } catch (JSONException e) {}
        try { setUsername(json.getString(getStringRessource(R.string.keyUsername))); } catch (JSONException e) {}
        try { setPubQos(json.getInt(getStringRessource(R.string.keyPubQos))); } catch (JSONException e) {}
        try { setKeepalive(json.getInt(getStringRessource(R.string.keyKeepalive))); } catch (JSONException e) {}
        try { setPubRetain(json.getBoolean(getStringRessource(R.string.keyPubRetain))); } catch (JSONException e) {}
        try { setTls(json.getInt(getStringRessource(R.string.keyTls))); } catch (JSONException e) {}
        try { setTlsCrtPath(json.getString(getStringRessource(R.string.keyTlsCrtPath))); } catch (JSONException e) {}
        try { setLocatorDisplacement(json.getInt(getStringRessource(R.string.keyLocatorDisplacement))); } catch (JSONException e) {}
        try { setLocatorInterval(json.getInt(getStringRessource(R.string.keyLocatorInterval))); } catch (JSONException e) {}
        try { setAuth(json.getBoolean(getStringRessource(R.string.keyAuth))); } catch (JSONException e) {}
        try { setPubIncludeBattery(json.getBoolean(getStringRessource(R.string.keyPubIncludeBattery))); } catch (JSONException e) {}
        try { setConnectionAdvancedMode(json.getBoolean(getStringRessource(R.string.keyConnectionAdvancedMode))); } catch (JSONException e) {}
        try { setSub(json.getBoolean(getStringRessource(R.string.keySub))); } catch (JSONException e) {}
        try { setPub(json.getBoolean(getStringRessource(R.string.keyPub))); } catch (JSONException e) {}
        try { setUpdateAdressBook(json.getBoolean(getStringRessource(R.string.keyUpdateAddressBook))); } catch (JSONException e) {}
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

    public static boolean getUpdateAdressBook() {
        return getBoolean(R.string.keyUpdateAddressBook,
                R.bool.valUpdateAddressBook);
    }

    public static String getTrackingUsername() {
        return getString(R.string.keyTrackingUsername, R.string.valEmpty);
    }

    public static void setTrackingUsername(String topic) {
        setString(R.string.keyTrackingUsername, topic);
    }

    public static int getLocatorDisplacement() {
        return getInt(R.string.keyLocatorDisplacement, R.integer.valLocatorDisplacement);
    }

    public static long getLocatorInterval() {
        return TimeUnit.MINUTES.toMillis(getInt(
                R.string.keyLocatorInterval,
                R.integer.valLocatorInterval));
    }


    public static String getUsername() {
        return getString(R.string.keyUsername, R.string.valEmpty);
    }

    public static boolean getAuth() {
        return getBoolean(R.string.keyAuth, R.bool.valAuth);

    }

    public static String getDeviceId(boolean androidIdFallback) {
        String name = getString(R.string.keyDeviceId, R.string.valEmpty);
        if (name.equals("") && androidIdFallback)
            name = App.getAndroidId();
        return name;
    }

    public static boolean getZeroLenghClientId() {
        return getBoolean(R.string.keyZeroLenghClientIdEnabled, R.bool.valZeroLenghClientIdEnabled);
    }

    public static void setZeroLenghClientId(boolean enabled) {
        Preferences.setBoolean(R.string.keyZeroLenghClientIdEnabled, enabled);
    }

    public static String getClientId(boolean androidIdFallback) {
        String name = getString(R.string.keyClientId, R.string.valEmpty);
        if (name.equals("") && androidIdFallback)
            name = App.getAndroidId();
        return name;
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

    public static String getPubTopicBase(boolean defaultFallback) {
        String topic = getString(R.string.keyPubTopicBase, R.string.valEmpty);
        if (topic.equals("") && defaultFallback)
            topic = getPubTopicFallback();

        return topic;
    }

    public static String getBaseTopic() {
        return getPubTopicBase(true);
    }

    public static String getPubTopicPartWaypoints() {
        return getStringRessource(R.string.valPubTopicPartWaypoints);
    }

    public static String getPubTopicFallback() {
        String deviceId = getDeviceId(true);
        String userUsername = getUsername();

        return deviceId.equals("") || userUsername.equals("") ? "" : String.format(getStringRessource(R.string.valPubTopicBase), userUsername, deviceId);
    }

    public static void setHost(String value) {
        if (!value.equals(getHost())) {
            brokerChanged();
            setString(R.string.keyHost, value);
        }
    }

    public static void setPort(int value) {
        if (value != getPort()) {
            setInt(R.string.keyPort, value);
            brokerChanged();
        }
    }

    public static int getPort() {
        return getInt(R.string.keyPort, R.integer.valPort);
    }


    public static void setKeepalive(int value) {
        setInt(R.string.keyKeepalive, value);
    }

    public static int getKeepalive() {
        return getInt(R.string.keyKeepalive, R.integer.valKeepalive);
    }

    public static void setUsername(String value) {
        if (!value.equals(getHost())) {

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

    private static void setUpdateAdressBook(boolean aBoolean) {
        setBoolean(R.string.keyUpdateAddressBook, aBoolean);
    }

    private static void setPubInterval(int anInt) {
        setInt(R.string.keyPubInterval, anInt);

    }

    private static void setPubTopicBase(String string) {
        setString(R.string.keyPubTopicBase, string);
        ServiceProxy.getServiceBroker().resubscribe();
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
        setString(R.string.keyPassword, password);
    }

    public static void setDeviceId(String deviceId) {
        setString(R.string.keyDeviceId, deviceId);
    }

    public static void setAuth(boolean auth) {
        setBoolean(R.string.keyAuth, auth);
    }

    public static void setTls(int tlsSpecifier) {
        setInt(R.string.keyTls, tlsSpecifier);
    }

    public static void setTlsCrtPath(String tlsCrtPath) {
        setString(R.string.keyTlsCrtPath, tlsCrtPath);
    }

    private static void brokerChanged() {
        Log.v("Preferences", "broker changed");
        EventBus.getDefault().post(new Events.BrokerChanged());
    }

    public static String getHost() {
        return getString(R.string.keyHost, R.string.valHost);
    }

    public static String getPassword() {
        return getString(R.string.keyPassword, R.string.valEmpty);
    }

    public static int getTls() {
        return getInt(R.string.keyTls, R.integer.valTls);
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

    public static String getBitcoinAddress() {
        return App.getContext().getString(R.string.valBitcoinAddress);

    }

    public static int getPortDefault() {
        return getIntResource(R.integer.valPort);
    }

    public static int getLocatorAccuracyForeground() {
        return getInt(R.string.keyLocatorAccuracyForeground, R.integer.valLocatorAccuracyForeground);
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

}
