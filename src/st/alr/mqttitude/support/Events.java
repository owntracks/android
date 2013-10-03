package st.alr.mqttitude.support;


import java.util.Date;

import android.location.Location;

public class Events {
    public static abstract class E {
        Date date;
        public E() {
            this.date = new Date();
        }
        public Date getDate() {
            return date;
        } 
       
    }
    
    public static class PublishSuccessfull extends E{
        Object extra;
        public PublishSuccessfull(Object extra) {
            this.extra = extra;
            this.date = new Date();
        }
        public Object getExtra() {
            return extra;
        }
    }
    
    public static class LocationUpdated  extends E{
        GeocodableLocation l; 
        
        public LocationUpdated(Location l) {
            this.l = new GeocodableLocation(l);
        }

        public LocationUpdated(GeocodableLocation l) {
            this.l = l;
        }

        public GeocodableLocation getGeocodableLocation() {
            return l;
        }

    }

    public static class ContactLocationUpdated  extends E{
        private String t; 
        private GeocodableLocation l; 

        public ContactLocationUpdated(Location l, String t) {
            this(new GeocodableLocation(l), t);
        }
        public ContactLocationUpdated(GeocodableLocation l, String t) {
            super();
            this.l = l;
            this.t = t;
        }


        public String getTopic(){
            return t;
        }
        public GeocodableLocation getGeocodableLocation() {
            return l;
        }
    }

    
    public static class StateChanged {
        public static class ServiceMqtt extends E{
            private Defaults.State.ServiceMqtt state;
            private Object extra;
            
            public ServiceMqtt(Defaults.State.ServiceMqtt state) {
               this(state, null);
            }
            
            public ServiceMqtt(Defaults.State.ServiceMqtt state, Object extra) {
                this.state = state;
                this.extra = extra;
            }
            public Defaults.State.ServiceMqtt getState() {
                return this.state;
            }
            public Object getExtra() {
                return extra;
            }
            
        }
        public static class ServiceLocator  extends E {
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
