package org.owntracks.android.messages;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;
import java.lang.ref.WeakReference;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageLocation extends MessageBase implements ILatLng {
    private String tid;
    private String t;
    private int bat;
    private int acc;
    private double lat;
    private double lon;
    private long tst;
    private String geocoder;
    private WeakReference<FusedContact> _contact;
    private LatLng point;


    @Override
    @JsonProperty("lat")
    public double getLatitude() {
        return lat;
    }

    @Override
    @JsonProperty("lon")
    public double getLongitude() {
        return lon;
    }

    @Override
    @JsonIgnore
    public double getAltitude() {
        return 0;
    }

    // JSON properties
    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
        notifyContactPropertyChanged();
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public int getBat() {
        return bat;
    }

    public void setBat(int bat) {
        this.bat = bat;
    }

    public int getAcc() {
        return acc;
    }

    public void setAcc(int acc) {
        this.acc = acc;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public long getTst() {
        return tst;
    }

    public void setTst(long tst) {
        this.tst = tst;
    }

    @JsonIgnore
    public String getGeocoder() {
        return geocoder != null ? geocoder : (getLatitude() + " : " + getLongitude());
    }

    @JsonIgnore
    public void setGeocoder(String geocoder) {
        this.geocoder = geocoder;
        notifyContactPropertyChanged();
    }

    public String getBaseTopicSuffix() {  return null; }

    public void setContact(FusedContact contact) {
        this._contact = new WeakReference<>(contact);
    }
    private void notifyContactPropertyChanged() {
        if(_contact != null && _contact.get() != null)
            this._contact.get().notifyMessageLocationPropertyChanged();

    }

    public LatLng getPoint() {
        return point;
    }
    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }

}
