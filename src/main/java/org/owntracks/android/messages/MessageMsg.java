package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutoingMessageProcessor;


@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageMsg extends MessageBase {
    public static final String BASETOPIC_SUFFIX = "/msg";
    @JsonIgnore
    private String icon = "fa-envelope-o";
    @JsonIgnore
    private String iconUrl;


    private double lat = 0;
    private double lon = 0;
    private String title;
    private String desc;
    private String url;
    private String channel;
    private int prio = 0;
    private int mttl = 0;
    private long tst;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getPrio() {
        return prio;
    }

    public void setPrio(int prio) {
        this.prio = prio;
    }

    public int getMttl() {
        return mttl;
    }

    public void setMttl(int mttl) {
        this.mttl = mttl;
    }

    public long getTst() {
        return tst;
    }

    public void setTst(long tst) {
        this.tst = tst;
    }

    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }
    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutoingMessageProcessor handler) {
        handler.processMessage(this);
    }

}
