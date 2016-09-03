package org.owntracks.android.support;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import android.util.Log;

import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.support.MessageWaypointCollection;

public class WaypointPair {
    // Class to decide where a location lies given a list of waypoints
    public static final String TAG = "WaypointPair";
    private String fromLoc, toLoc;
    private int progress, rate;

    public void WaypointPair() {
    }

    public void setPath(String from, String to) {
        fromLoc = from; toLoc = to;
    }
    
    public void setProgress(int p) {
        progress = p;
    }

    public String getFromLoc() { return fromLoc; }
    public String getToLoc() { return toLoc; }
    public int getProgress() { return progress; }

    public static WaypointPair getWaypointPair(MessageLocation loc,
                                               MessageWaypointCollection wayps) {
        Object [] waypArr = wayps.toArray();
        int i;
        double minPeri = (double)Integer.MAX_VALUE;
        MessageWaypoint selW1, selW2;

        selW1 = null; selW2 = null;
        for (i = 0; i < (wayps.size() - 1); i++) {
            int j;
            for (j = (i + 1); j < wayps.size(); j++) {
                MessageWaypoint w1, w2;
                w1 = (MessageWaypoint)waypArr[i];
                w2 = (MessageWaypoint)waypArr[j];

                //Log.d(TAG, "Matching " + loc.getLatitude() + "," + loc.getLongitude() +
                //      " with waypoint " + w1.getDesc() + ":" + w1.getLat() + "," + w1.getLon() +
                //      " and " + w2.getDesc() + ":" + w2.getLat() + "," + w2.getLon());
                if (locInBound(loc, w1, w2)) {
                    double peri = manhattanDist(w1.getLat(), w1.getLon(), w2.getLat(), w2.getLon());
                    if (peri < minPeri) {
                        minPeri = peri;
                        selW1 = w1; selW2 = w2;
                    }
                    //Log.d(TAG, "in bounds with peri " + peri);
                }
            }
        }

        WaypointPair ret;
        if (minPeri != (double)Integer.MAX_VALUE) {
            ret = new WaypointPair();
            Double dist, totalDist;
            ret.setPath(selW1.getDesc(), selW2.getDesc());
            dist = manhattanDist(loc.getLatitude(), loc.getLongitude(),
                                 selW1.getLat(), selW1.getLon());
            totalDist = manhattanDist(selW1.getLat(), selW1.getLon(),
                                      selW2.getLat(), selW2.getLon());
            // Convert ratio to %-age
            dist = (dist / totalDist) * 100;
            ret.setProgress(dist.intValue());
            //Log.d(TAG, "Matched wayp " + selW1 + " to loc " + loc + ", dist " + dist);
        } else {
            ret = null;
        }
        
        return ret;
    }

    private static boolean locInBound(MessageLocation loc,
                                  MessageWaypoint w1, MessageWaypoint w2) {
        double chkLat, chkLon;
        double [] bbox; // (top left, bottom right) x (lat, lon)

        chkLat = loc.getLatitude(); chkLon = loc.getLongitude();
        bbox = new double[4];

        bbox[0] = Math.min(w1.getLat(), w2.getLat()) - BBOX_TOLERANCE;
        bbox[1] = Math.min(w1.getLon(), w2.getLon()) - BBOX_TOLERANCE;
        bbox[2] = Math.max(w1.getLat(), w2.getLat()) + BBOX_TOLERANCE;
        bbox[3] = Math.max(w1.getLon(), w2.getLon()) + BBOX_TOLERANCE;

        //Log.d(TAG, "Bounds " + Arrays.toString(bbox));
        
        if (chkLat >= bbox[0] && chkLat <= bbox[2] &&
            chkLon >= bbox[1] && chkLon <= bbox[3])
            return true;
        else
            return false;
    }

    private static double manhattanDist(double lat1, double lon1,
                                       double lat2, double lon2) {
        double dist;
        double [] dbg = { lat1, lon1, lat2, lon2 };
        dist = (Math.abs(lat1 - lat2) + Math.abs(lon1 - lon2));
        //Log.d(TAG, "manht dist " + Arrays.toString(dbg) + "=" + dist);
        return dist;
    }

    private static final double BBOX_TOLERANCE = 0.001; // approx 100m at equator
}
