package st.alr.mqttitude.support;

import android.location.Location;
import st.alr.mqttitude.services.ServiceMqtt;

public class Events {
    public static class PublishSuccessfull {

    }
    public static class LocationUpdated {
        Location l; 
        
        public LocationUpdated(Location l) {
            this.l = l;
        }

        public Location getLocation() {
            return l;
        }
        
        
    }
    public static class MqttConnectivityChanged {
		private ServiceMqtt.MQTT_CONNECTIVITY connectivity;

		public MqttConnectivityChanged(
				ServiceMqtt.MQTT_CONNECTIVITY connectivity) {
			this.connectivity = connectivity;
		}

		public ServiceMqtt.MQTT_CONNECTIVITY getConnectivity() {
			return connectivity;
		}
	}
    
    public static class StateChanged {}
}
