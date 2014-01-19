package st.alr.mqttitude.support;


import java.util.Date;

import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.model.GeocodableLocation;


import android.location.Location;

public class Events {

 

    public static class WaypointUpdated extends E{
        Waypoint w;
        
        public WaypointUpdated(Waypoint w) {
            super();
            this.w = w;
        }
        public Waypoint getWaypoint() {
            return w;
        }

    }

    public static class WaypointTransition extends E{
        Waypoint w;
        int transition; 
        
        public WaypointTransition(Waypoint w, int transition) {
            super();
            this.w = w;
            this.transition = transition;
        }
        public Waypoint getWaypoint() {
            return w;
        }
        
        public int getTransition(){
            return transition; 
        }

    }
    public static class WaypointAdded extends E{
        Waypoint w;
        public WaypointAdded(Waypoint w) {
            super();
            this.w = w;
        }
        public Waypoint getWaypoint() {
            return w;
        }

    }
    public static class WaypointRemoved extends E{
        Waypoint w;
        public WaypointRemoved(Waypoint w) {
            super();
            this.w = w;
        }
        public Waypoint getWaypoint() {
            return w;
        }

    }

    
    public static abstract class E {
        Date date;
        public E() {
            this.date = new Date();
        }
        public Date getDate() {
            return date;
        } 
       
    }
    public static class Dummy extends E{
        public Dummy() {
        }
    }

    
    public static class PublishSuccessfull extends E{
        Object extra;
        public PublishSuccessfull(Object extra) {
            super();
            this.extra = extra;
        }
        public Object getExtra() {
            return extra;
        }
    }
    
    public static class LocationUpdated extends E{
        GeocodableLocation l; 
        
        public LocationUpdated(Location l) {
            super();
            this.l = new GeocodableLocation(l);
        }

        public LocationUpdated(GeocodableLocation l) {
            this.l = l;
        }

        public GeocodableLocation getGeocodableLocation() {
            return l;
        }

    }

    
    public static class ContactAdded extends E {
        Contact contact; 
        public ContactAdded(Contact f) {
            super(); 
            this.contact = f;
        }
        public Contact getContact() {
            return contact;
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
    
    public static class ContactUpdated extends E{
        private Contact c; 
        public ContactUpdated(Contact c) {
            super();
            this.c = c;             
        }
        public Contact getContact() {
            return c;
        }
               
    }


    
    public static class StateChanged {
        public static class ServiceBroker extends E{
            private Defaults.State.ServiceBroker state;
            private Object extra;
            
            public ServiceBroker(Defaults.State.ServiceBroker state) {
               this(state, null);
            }
            
            public ServiceBroker(Defaults.State.ServiceBroker state, Object extra) {
                super();
                this.state = state;
                this.extra = extra;
            }
            public Defaults.State.ServiceBroker getState() {
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
