package st.alr.mqttitude.support;

import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Defaults {
	public static final String INTENT_ACTION_PUBLISH_LASTKNOWN = "st.alr.mqttitude.intent.PUB_LASTKNOWN";
	public static final String INTENT_ACTION_PUBLISH_PING = "st.alr.mqttitude.intent.PUB_PING";
	public static final String INTENT_ACTION_LOCATION_CHANGED = "st.alr.mqttitude.intent.LOCATION_CHANGED";
	public static final String INTENT_ACTION_FENCE_TRANSITION = "st.alr.mqttitude.intent.FENCE_TRANSITION";
	public static final int NOTIFCATION_ID = 1338;

    public static String formatNotificationMessage(Context c, String formatString, boolean enter, Waypoint w) {
        if(enter)
            formatString = formatString.replace("%d", w.getDescription()).replace("%e", c.getString(R.string.transitionEnteringNotification));
        else
            formatString = formatString.replace("%d", w.getDescription()).replace("%e", c.getString(R.string.transitionLeavingNotification));

        formatString =  formatString.replaceAll("%g", w.getGeocoder());
        formatString =  formatString.replaceAll("%lat", w.getLatitude().toString());
        formatString = formatString.replaceAll("%lon", w.getLongitude().toString());
        formatString = formatString.replaceAll("%r", w.getRadius().toString());
        return formatString.substring(0, 1).toUpperCase() + formatString.substring(1); // Capitalize beginning
    }

	public static class TransitionType {
		public static String toString(int type, Context c) {
			int id;
			switch (type) {
			case 0:
				id = R.string.transitionEnter;
				break;
			case 1:
				id = R.string.transitionLeave;
				break;
			case 2:
				id = R.string.transitionBoth;
				break;
			default:
				id = R.string.transitionEnter;
			}
			return c.getString(id);
		}
	}

	public static class State {
		public static enum ServiceBroker {
			INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED_ERROR
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
            INITIAL, PUBLISHING, PUBLISHING_WAITING, PUBLISHING_TIMEOUT, NOTOPIC, NOLOCATION
        }

        public static String toString(
                st.alr.mqttitude.support.Defaults.State.ServiceLocator state,
                Context c) {
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

        public static enum ServiceBeacon {
            INITIAL, PUBLISHING, PUBLISHING_WAITING, PUBLISHING_TIMEOUT, NOTOPIC, NOBLUETOOTH
        }

        public static String toString(
                st.alr.mqttitude.support.Defaults.State.ServiceBeacon state,
                Context c) {
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
                case NOBLUETOOTH:
                    id = R.string.stateBluetoothFail;
                    break;
                default:
                    id = R.string.stateIdle;
            }

            return c.getString(id);
        };

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

}
