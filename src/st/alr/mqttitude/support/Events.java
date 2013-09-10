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
    
    public static class StateChanged {
        public static class ServiceMqtt {
            private Defaults.State.ServiceMqtt state;
            public ServiceMqtt(Defaults.State.ServiceMqtt state) {
                this.state = state;
            }
            public Defaults.State.ServiceMqtt getState() {
                return this.state;
            }
            
        }
        public static class ServiceLocator {
            private Defaults.State.ServiceLocator state;
            public ServiceLocator(Defaults.State.ServiceLocator state) {
                this.state = state;
            }
            public Defaults.State.ServiceLocator getState() {
                return this.state;
            }
            
        }
   }
}
