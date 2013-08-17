package st.alr.mqttitude.support;

public class Defaults {

    public static final int NOTIFCATION_ID = 1338;
    public static final String  UPDATE_INTEND_ID = "st.alr.mqttitude.UPDATE";


    public static final String SETTINGS_KEY_BROKER_HOST = "brokerHost";
    public static final String SETTINGS_KEY_BROKER_PORT = "brokerPort";    
    public static final String SETTINGS_KEY_BROKER_PASSWORD = "brokerPassword";
    public static final String SETTINGS_KEY_BROKER_USERNAME = "brokerUsername";
    public static final String SETTINGS_KEY_BROKER_SECURITY = "brokerSecurity";
    public static final String SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH = "brokerSecuritySslCaPath";
    public static final String SETTINGS_KEY_UPDATE_INTERVAL = "backgroundUpdateInterval";

    public static final String SETTINGS_KEY_NOTIFICATION_ENABLED = "notificationEnabled";
    
    public static final String VALUE_BROKER_HOST = "192.168.8.2";
    public static final String VALUE_BROKER_PORT = "1883";
    public static final String VALUE_UPDATE_INTERVAL = "30";

    
    public static final int VALUE_BROKER_SECURITY_NONE = 0;
    public static final int VALUE_BROKER_SECURITY_SSL = 1;
    public static final int VALUE_BROKER_SECURITY_SSL_CUSTOMCACRT = 2;

    public static final boolean VALUE_NOTIFICATION_ENABLED = true;

    public enum State {Idle, Locating, LocatingFail, PublishConnectionWaiting, PublishConnectionTimeout, Publishing, NOTOPIC};
}
