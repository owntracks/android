package org.owntracks.android.messages;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;
import java.lang.ref.WeakReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.android.gms.maps.model.LatLng;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageLocation extends MessageBase  {
    public static final String REPORT_TYPE_USER = "u";
    public static final String REPORT_TYPE_RESPONSE = "r";
    private String t;
    private int batt;
    private int acc;
    private double lat;
    private double lon;
    private long tst;
    private String geocoder;
    private WeakReference<FusedContact> _contact;
    private LatLng point;


    @JsonProperty("lat")
    public double getLatitude() {
        return lat;
    }

    @JsonProperty("lon")
    public double getLongitude() {
        return lon;
    }

    @JsonIgnore
    public double getAltitude() {
        return 0;
    }



    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public int getBatt() {
        return batt;
    }

    public void setBatt(int batt) {
        this.batt = batt;
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

    @JsonIgnore
    public String getBaseTopicSuffix() {  return null; }

    public void setContact(FusedContact contact) {
        this._contact = new WeakReference<>(contact);
    }
    private void notifyContactPropertyChanged() {
        if(_contact != null && _contact.get() != null)
            this._contact.get().notifyMessageLocationPropertyChanged();

    }

    @JsonIgnore
    public LatLng getLatLng() {
        return point != null ? point : (point = new LatLng(lat, lon));
    }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }

    public void setTid(String tid) {
        super.setTid(tid);
        notifyContactPropertyChanged();
    }

}
