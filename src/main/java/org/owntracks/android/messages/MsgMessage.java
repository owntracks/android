package org.owntracks.android.messages;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class MsgMessage extends Message {
    private static final String TAG = "MsgMessage";
    private double lat = 0;
    private double lon = 0;
    private String title;
    private String desc;
    private String icon = "fa-envelope-o";
    private String iconUrl;
    private String url;
    private String channel;


    private int prio = 0;
    private int mttl = 0;
    private long tst;

    public MsgMessage(JSONObject json) throws JSONException{
        super();

            title = json.getString("title");
            desc = json.getString("desc");

        try { lat = json.getLong("lat"); } catch (JSONException e) {}
        try { lon = json.getLong("lon"); } catch (JSONException e) {}
        try { icon = json.getString("icon"); } catch (JSONException e) {}
        try { iconUrl = json.getString("iconUrl"); } catch (JSONException e) {}
        try { prio = json.getInt("prio"); } catch (JSONException e) {}
        try { mttl = json.getInt("ttl"); } catch (JSONException e) {}
        try { tst = json.getLong("tst"); } catch (JSONException e) {}
        try { url = json.getString("url"); } catch (JSONException e) {}


    }

    public String getChannel() {return channel;}

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public String getIcon() {
        if(icon != null && !"".equals(icon) )
            return icon;
        else
            return "fa-envelope-o";
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public int getPrio() {
        if( prio > 0 && prio < 3)
            return prio;
        else
            return 0;
    }

    public int getMttl() {
        return mttl;
    }

    public boolean expires() {
        return mttl != 0;
    }

    public boolean isExpired() {
        return expires() && TimeUnit.SECONDS.toMillis(this.mttl) <= System.currentTimeMillis();
    }

    public long getTst() {
        return tst;
    }
    public String getUrl() {
        return url;
    }

}
