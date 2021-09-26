package org.owntracks.android.support.interfaces;


public interface StatefulServiceMessageProcessor {
    void reconnect();
    boolean checkConnection();
}
