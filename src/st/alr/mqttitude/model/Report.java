package st.alr.mqttitude.model;

import st.alr.mqttitude.App;
import st.alr.mqttitude.preferences.ActivityPreferences;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class Report {
    GeocodableLocation location;
    int event;
    public static final int EVENT_NONE = 0;
    public static final int EVENT_ENTER = 1;
    public static final int EVENT_LEAVE = 2;
    
    public Report(GeocodableLocation l){
        this(l, EVENT_NONE);
    }
    
    public Report(GeocodableLocation l, int event){
        this.location = l;
        this.event = event;
    }
   

    public String toString(){
        StringBuilder builder = new StringBuilder();
        
        builder.append("{");
        builder.append("\"_type\": ").append("\"").append("location").append("\"");
        builder.append(", \"lat\": ").append("\"").append(location.getLatitude()).append("\"");
        builder.append(", \"lon\": ").append("\"").append(location.getLongitude()).append("\"");
        builder.append(", \"tst\": ").append("\"").append((int)(location.getTime()/1000)).append("\"");
        builder.append(", \"acc\": ").append("\"").append(Math.round(location.getLocation().getAccuracy() * 100) / 100.0d).append("\"");

        if(ActivityPreferences.includeBattery())
            builder.append(", \"batt\": ").append("\"").append(App.getBatteryLevel()).append("\"");

        return builder.append("}").toString();
        
            
    }

}
