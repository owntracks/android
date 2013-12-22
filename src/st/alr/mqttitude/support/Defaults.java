package st.alr.mqttitude.support;

import android.content.Context;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceApplication;

public class Defaults {

    public static final int NOTIFCATION_ID = 1338;
    public static final String  UPDATE_INTEND_ID = "st.alr.mqttitude.UPDATE";


    public static final String SETTINGS_KEY_BROKER_HOST = "brokerHost";
    public static final String SETTINGS_KEY_BROKER_PORT = "brokerPort";    
    public static final String SETTINGS_KEY_DEVICE_NAME = "deviceName";
    public static final String SETTINGS_KEY_BROKER_PASSWORD = "brokerPassword";
    public static final String SETTINGS_KEY_USER_NAME = "userUsername";
    public static final String SETTINGS_KEY_BROKER_AUTH = "brokerAuth";

    public static final String SETTINGS_KEY_BROKER_SECURITY = "brokerSecurity";
    public static final String SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH = "brokerSecuritySslCaPath";
    public static final String SETTINGS_KEY_BACKGROUND_UPDATES = "backgroundUpdates";
    public static final String SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL = "backgroundUpdatesInterval";

    public static final String SETTINGS_KEY_TOPIC_PUB = "topic";
    public static final String SETTINGS_KEY_RETAIN = "retain";
    public static final String SETTINGS_KEY_QOS = "qos";
    public static final String SETTINGS_KEY_NOTIFICATION_ENABLED = "notificationEnabled";
    public static final String SETTINGS_KEY_TICKER_ON_PUBLISH = "notificationTickerOnPublishEnabled";
    
    public static final String VALUE_BROKER_HOST = "192.168.8.2";
    public static final String VALUE_BROKER_PORT = "8883";
    public static final String VALUE_BACKGROUND_UPDATES_INTERVAL = "15";
    public static final boolean VALUE_BACKGROUND_UPDATES = false;

    public static final int VALUE_BROKER_SECURITY_NONE = 0;
    public static final int VALUE_BROKER_SECURITY_SSL = 1;
    public static final int VALUE_BROKER_SECURITY_SSL_CUSTOMCACRT = 2;
    public static final boolean VALUE_NOTIFICATION_ENABLED = true;
    public static final String VALUE_TOPIC_PUB = "mqttitude/%s/%s";




    public static final boolean VALUE_RETAIN = false;
    public static final String VALUE_QOS = "0";
    public static final boolean VALUE_TICKER_ON_PUBLISH = true;
    public static final String BUGSNAG_API_KEY = "f3302f4853372edcdd12dfcc102a3578";
    public static final String VALUE_REPO_URL = "http://github.com/binarybucks/mqttitude";
    public static final String VALUE_ISSUES_MAIL = "issues@mqttitude.org";
    public static final String INTENT_ACTION_PUBLISH_LASTKNOWN = "st.alr.mqttitude.intent.PUB_LASTKNOWN";
    public static final String INTENT_ACTION_PUBLICH_PING = "st.alr.mqttitude.intent.PUB_PING";
    public static final String VALUE_TOPIC_SUB = "mqttitude/+/+";
    
    public static final int VALUE_BROKER_AUTH_ANONYMOUS = 0;
    public static final int VALUE_BROKER_AUTH_USERNAME = 1;
    public static final String SETTINGS_KEY_TRACKING = "tracking";
    public static final String SETTINGS_KEY_CONTACTS = "subEnabled";
    public static final String SETTINGS_KEY_TOPIC_SUB = "subTopic";
    public static final String CURRENT_LOCATION_TRACKING_IDENTIFIER = "+CURRENTDEVICELOCATION+";


    public static class State {
        public static enum ServiceBroker {
            INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED_WAITINGFORINTERNET, DISCONNECTED, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED_ERROR
        }
        public static String toString(ServiceBroker state, Context c) {
            int id; 
            switch (state) {
                case CONNECTED:
                    id = R.string.connectivityConnected;
                    break;
                case CONNECTING:
                    id = R.string.connectivityConnecting;
                    break;
                case DISCONNECTING:
                    id = R.string.connectivityDisconnecting;
                    break;
                case DISCONNECTED_USERDISCONNECT:
                    id = R.string.connectivityDisconnectedUserDisconnect;
                    break;
                case DISCONNECTED_DATADISABLED:
                    id = R.string.connectivityDisconnectedDataDisabled;
                    break;
                case DISCONNECTED_ERROR:
                    id = R.string.error;
                    break;
                default:
                    id = R.string.connectivityDisconnected;
                    
            }
            return c.getString(id);
        }
        
        public static enum ServiceLocator {
            INITIAL, PUBLISHING, PUBLISHING_WAITING, PUBLISHING_TIMEOUT, NOTOPIC, NOLOCATION}
        
            public static String toString(st.alr.mqttitude.support.Defaults.State.ServiceLocator state, Context c) {
                int id; 
                switch (state) {
                    case PUBLISHING:
                        id = R.string.statePublishing;
                        break;
                    case PUBLISHING_WAITING:
                        id = R.string.stateWaiting;                                  
                        break;
                    case PUBLISHING_TIMEOUT:
                        id = R.string.statePublishTimeout;
                        break;
                    case NOTOPIC:
                        id = R.string.stateNotopic;
                        break;
                    case NOLOCATION: 
                        id = R.string.stateLocatingFail;    
                        break;
                    default:
                        id = R.string.stateIdle;
                }
                
                return c.getString(id);
            };
        
    }
    

    
}
