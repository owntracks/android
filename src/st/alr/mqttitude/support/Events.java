package st.alr.mqttitude.support;


import java.util.Date;

import android.location.Location;
import st.alr.mqttitude.services.ServiceMqtt;

public class Events {
    public static class PublishSuccessfull {
        Object extra;
        Date date;
        public PublishSuccessfull(Object extra) {
            this.extra = extra;
            this.date = new Date();
        }
        public Object getExtra() {
            return extra;
        }
        public Date getDate() {
            return date;
        }

    }
    
    public static class LocationUpdated {
        GeocodableLocation l; 
        
        public LocationUpdated(GeocodableLocation l) {
            this.l = l;
        }

        public GeocodableLocation getGeocodableLocation() {
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
