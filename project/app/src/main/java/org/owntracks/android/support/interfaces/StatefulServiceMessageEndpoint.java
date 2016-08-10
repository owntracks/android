package org.owntracks.android.support.interfaces;


public interface StatefulServiceMessageEndpoint extends ServiceMessageEndpoint {
    void reconnect();
    void disconnect();
}
