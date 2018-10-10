package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.android.gms.maps.model.LatLng;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

import java.lang.ref.WeakReference;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageLocation extends MessageBase {
    public static final String TYPE = "location";
    public static final String REPORT_TYPE_USER = "u";
    public static final String REPORT_TYPE_RESPONSE = "r";
    public static final String REPORT_TYPE_CIRCULAR = "c";
    public static final String REPORT_TYPE_PING = "p";
    public static final String REPORT_TYPE_DEFAULT = null;

    public static final String CONN_TYPE_OFFLINE = "o";
    public static final String CONN_TYPE_WIFI = "w";
    public static final String CONN_TYPE_MOBILE = "m";

    private String t;
    private int batt;
    private int acc;
    private int vac;
    private double lat;
    private double lon;
    private double alt;
    private double vel;
    private long tst;
    private String geocoder;
    private WeakReference<FusedContact> _contact;
    private LatLng point;
    private String conn;
    private List<String> inregions;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonProperty("_cp")
    private boolean cp = false;

    @JsonProperty("inregions")
    public List<String> getInRegions() {
        return inregions;
    }

    @JsonProperty("inregions")
    public void setInRegions(List<String> inregions) {
        this.inregions = inregions;
    }

    public boolean getCp() {
        return cp;
    }

    public void setCp(boolean cp) {
        this.cp = cp;
    }

    @JsonProperty("lat")
    public double getLatitude() {
        return lat;
    }

    @JsonProperty("lon")
    public double getLongitude() {
        return lon;
    }

    @JsonProperty("vel")
    public double getVelocity() {
        return vel;
    }

    public void setVelocity(double vel) {
        this.vel = vel;
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
        return hasGeocoder() ? geocoder : getGeocoderFallback();
    }

    @JsonIgnore
    public String getGeocoderFallback() {
        return (getLatitude() + " : " + getLongitude());
    }

    @JsonIgnore
    public boolean hasGeocoder() {
        return geocoder != null;
    }

    @JsonIgnore
    public void setGeocoder(String geocoder) {
        this.geocoder = geocoder;
        notifyContactPropertyChanged();
    }

    @JsonIgnore
    public String getBaseTopicSuffix() {
        return null;
    }

    public void setContact(FusedContact contact) {
        this._contact = new WeakReference<>(contact);
    }

    private void notifyContactPropertyChanged() {
        if (_contact != null && _contact.get() != null)
            this._contact.get().notifyMessageLocationPropertyChanged();

    }

    @JsonIgnore
    public LatLng getLatLng() {
        return point != null ? point : (point = new LatLng(lat, lon));
    }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processOutgoingMessage(this);
    }

    public void setTid(String tid) {
        super.setTid(tid);
        notifyContactPropertyChanged();
    }

    public boolean isValidMessage() {
        return tst > 0;
    }

    public void setConn(String conn) {
        this.conn = conn;
    }

    public String getConn() {
        return this.conn;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }

    public double getAlt() {
        return alt;
    }

    public void setVac(int vac) {
        this.vac = vac;
    }

    public int getVac() {
        return this.vac;
    }
}

