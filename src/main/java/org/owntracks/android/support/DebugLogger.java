package org.owntracks.android.support;

import android.util.Log;

import org.owntracks.android.BuildConfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

/**
 * Created by aru on 26.02.2015.
 */
public class DebugLogger {
    private LinkedList<String> list;
    public DebugLogger() {
        this.list = new LinkedList<String>();
    }

    public void v(String id, String log) {
        Log.v(id, log);
        if(!BuildConfig.DEBUG)
            return;
        if(list.size() > 5000)
            list.clear();

        this.list.push(new Date() + ":" +log);
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        for (String log : this.list) {
            s.append(log);
            s.append("\n");
        }
        return s.toString();
    }

    public void clear() {
        this.list.clear();
    }
}
