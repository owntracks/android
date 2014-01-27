package st.alr.mqttitude.support;

import java.util.concurrent.TimeUnit;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    
    
    public static SharedPreferences getSharedPreferences(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext());
    }
    
    public static String getKey(int resId){
        return App.getContext().getString(resId);
    }

    public static boolean getBoolean(int resId, boolean defValue){
        return getSharedPreferences().getBoolean(getKey(resId), defValue);
    }
    public static int getInt(int resId, int defValue){
        return getSharedPreferences().getInt(getKey(resId), defValue);
    }
    public static String getString(int resId, String defValue){
        String s = getSharedPreferences().getString(getKey(resId), defValue);
        if(s == null || s.equals(""))
            s = defValue;
        
        return s;
    }
    public static void setString(int resId, String value){
        getSharedPreferences().edit().putString(getKey(resId), value).apply();
    }
    
    public static void setInt(int resId, int value) {
        getSharedPreferences().edit().putInt(getKey(resId), value).apply();
    }

    
    public static String getAndroidId(){
        return App.getAndroidId();
    }
    
    public static boolean isAdvancedModeEnabled() {
        return getBoolean(R.string.keyAdvancedConnectionPreferencesEnabled, false);
    }

    public static String getServerAdress() {
        return getString(R.string.keyBrokerHost, "");
    }

    public static boolean includeBattery() {
        return getBoolean(R.string.keyPubIncludeBattery, false);
    }

    public static boolean isSubEnabled() {
        return getBoolean(R.string.keySubEnabled, true);
    }

    public static boolean isContactLinkCloudStorageEnabled() {
        return getBoolean(R.string.keyContactsLinkCloudStorageEnabled, false);
    }

    public static String getTrackingUsername() {
        return getString(R.string.keyTrackingUsername, "");
    }

    public static int getLocatorBackgroundDisplacement() {
        try {
            return Integer.parseInt(getString(R.string.keyLocatorBackgroundDisplacement, "500"));
        } catch (Exception e) {
            return 500;
        }
    }

    public static long getLocatorBackgroundInterval() {
        try {
            return TimeUnit.MINUTES.toMillis(Long.parseLong(getString(R.string.keyLocatorBackgroundInterval, "30")));
        } catch (Exception e) {
            return 30;
        }

    }

    
    public static void setTrackingUsername(String topic) {
        setString(R.string.keyTrackingUsername, topic);
    }

    
    public static String getBrokerUsername() {
        return getString(R.string.keyBrokerUsername, "");
    }

    public static int getBrokerAuthType() {
        return getInt(R.string.keyBrokerAuth, Defaults.VALUE_BROKER_AUTH_USERNAME);

    }
    
    public static String getDeviceName(boolean androidIdFallback)
    {
        String name = getString(R.string.keyDeviceName, "");
        if (name.equals("") && androidIdFallback)
            name = App.getAndroidId();
        return name;
    }

    public static String getSubTopic(boolean defaultTopicFallback) {
        String topic = getString(R.string.keySubTopic, "");
        if (topic.equals("") && defaultTopicFallback)
            topic = Defaults.VALUE_TOPIC_SUB;
        return topic;
    }

    public static String getPubTopic(boolean defaultFallback) {
        String topic = getString(R.string.keyPubTopic, "");
        if (topic.equals("") && defaultFallback)
            topic = getPubTopicFallback();

        return topic;
    }
    
    public static String getPubTopicFallback() {
        String deviceName = getDeviceName(true);
        String userUsername = getBrokerUsername();

        return deviceName.equals("") || userUsername.equals("") ? "" : String.format(Defaults.VALUE_TOPIC_PUB_BASE, userUsername, deviceName);
    }

    public static String getBrokerHost() {
        return getString(R.string.keyBrokerHost, "192.168.8.2");
    }

    public static String getBrokerPort() {
        return getString(R.string.keyBrokerPort, "8883");
    }

    public static String getBrokerPassword() {
        return getString(R.string.keyBrokerPassword, "");
    }

    public static int getBrokerSecurityType() {
        return getInt(R.string.keyBrokerSecurity, Defaults.VALUE_BROKER_SECURITY_SSL);
    }

    public static String getBrokerSslCaPath() {
        return getString(R.string.keyBrokerSecuritySslCaPath, "");
    }

    public static boolean isNotificationEnabled() {
        return getBoolean(R.string.keyNotificationEnabled, true);
    }

    public static boolean notificationOnGeofenceTransition() {
        return getBoolean(R.string.keyNotificationTickerGeofence, true);
    }

    public static boolean notificationTickerOnPublish() {
        return getBoolean(R.string.keyNotificationTickerPublish, true);
    }

    public static boolean isNotificationGeocoderEnabled(){
        return getBoolean(R.string.keyNotificationGeocoderEnabled, true);
    }
    
    public static boolean isNotificationLocationEnabled() {
        return getBoolean(R.string.keyNotificationLocationEnabled, true);
    }


    
    public static int getPubQos() {
        try {
            return Integer.parseInt(getString(R.string.keyPubQos, "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean getPubRetain() {
        return getBoolean(R.string.keyPubRetain, true);
    }
    
    public static int getPubAutoInterval(){
        int ui;
        try {
            ui = Integer.parseInt(getString(R.string.keyPubAutoInterval, "30"));
        } catch (Exception e) {
            ui = 30;
        }

        return ui;

    }

    public static boolean isPubAutoEnabled() {
        return getBoolean(R.string.keyPubAutoEnabled, false);
    }

    public static boolean isAutostartOnBootEnabled() {
        return getBoolean(R.string.keyAutostartOnBootEnabled, false);
    }

}
