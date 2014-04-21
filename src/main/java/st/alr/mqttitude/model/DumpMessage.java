package st.alr.mqttitude.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import st.alr.mqttitude.support.Defaults;

public class DumpMessage {
    LocationMessage location;
    ConfigurationMessage configuration;

    boolean locatorReady;
    boolean locatorForeground;
    GeocodableLocation locatorLastKnownLocation;
    Long locatorLastPublishDate;
    Integer locatorWaypointCount;
    boolean locatorHasLocationClient;
    boolean locatorHasLocationRequest;
    Defaults.State.ServiceLocator locatorState;

    Short brokerKeepAliveSeconds;
    Exception brokerError;
    Integer brokerDeferredPublishablesCount;
    Defaults.State.ServiceBroker brokerState;

    boolean applicationPlayServicesAvailable;

    public void setLocation(LocationMessage location) {
        this.location = location;
    }

    public void setConfiguration(ConfigurationMessage configuration) {
        this.configuration = configuration;
    }

    public void setLocatorReady(boolean locatorReady) {
        this.locatorReady = locatorReady;
    }

    public void setLocatorForeground(boolean locatorForeground) {
        this.locatorForeground = locatorForeground;
    }

    public void setLocatorLastKnownLocation(GeocodableLocation locatorLastKnownLocation) {
        this.locatorLastKnownLocation = locatorLastKnownLocation;
    }

    public void setLocatorLastPublishDate(Long locatorLastPublishDate) {
        this.locatorLastPublishDate = locatorLastPublishDate;
    }

    public void setLocatorWaypointCount(Integer locatorWaypointCount) {
        this.locatorWaypointCount = locatorWaypointCount;
    }

    public void setLocatorHasLocationClient(boolean locatorHasLocationClient) {
        this.locatorHasLocationClient = locatorHasLocationClient;
    }

    public void setLocatorHasLocationRequest(boolean locatorHasLocationRequest) {
        this.locatorHasLocationRequest = locatorHasLocationRequest;
    }

    public void setBrokerKeepAliveSeconds(Short brokerKeepAliveSeconds) {
        this.brokerKeepAliveSeconds = brokerKeepAliveSeconds;
    }

    public void setBrokerError(Exception brokerError) {
        this.brokerError = brokerError;
    }
    public void setBrokerDeferredPublishablesCount(Integer deferedPublishablesCount) {
        this.brokerDeferredPublishablesCount = deferedPublishablesCount;
    }

    public void setApplicationPlayServicesAvailable(boolean applicationPlayServicesAvailable) {
        this.applicationPlayServicesAvailable = applicationPlayServicesAvailable;
    }

    public void setLocatorState(Defaults.State.ServiceLocator locatorState) {
        this.locatorState = locatorState;
    }

    public void setBrokerState(Defaults.State.ServiceBroker brokerState) {
        this.brokerState = brokerState;
    }


    public String toString() {
        return toJsonObject().toString();
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("_type", "dump")
                .put("location", location.toJSONObject())
                .put("configuration", configuration.toJSONObject())
                .put("internal", new JSONObject()
                        .put("locator", new JSONObject()
                                .put("ready", locatorReady)
                                .put("foreground", locatorForeground)
                                .put("lastKnownLocation", locatorLastKnownLocation != null ? locatorLastKnownLocation : null)
                                .put("lastPublishDate", locatorLastPublishDate != null ? locatorLastPublishDate : null)
                                .put("waypointCount", locatorWaypointCount)
                                .put("hasLocationClient", locatorHasLocationClient)
                                .put("hasLocationRequest", locatorHasLocationRequest)
                                .put("state", locatorState)
                        )
                        .put("broker", new JSONObject()
                                .put("keepAliveSeconds", brokerKeepAliveSeconds)
                                .put("error", brokerError != null ? brokerError.toString() : null)
                                .put("deferredPublishablesCount", brokerDeferredPublishablesCount)
                                .put("state", brokerState)
                        )
                        .put("application", new JSONObject()
                                .put("playServicesAvailable", applicationPlayServicesAvailable)
                        )
                );


        } catch (JSONException e) {
            Log.e(this.toString(), e.toString());

        }
        return json;
    }
}
