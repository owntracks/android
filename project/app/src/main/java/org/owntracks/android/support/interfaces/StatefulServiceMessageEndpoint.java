package org.owntracks.android.support.interfaces;


public interface StatefulServiceMessageEndpoint extends ServiceMessageEndpoint {
    void reconnect();
    void disconnect();

    /**
     * Registers a connection listener. The same listener can only be added once, if executed multiple times on the same listener, nothing happens
     * @param listener to be added
     * @throws  NullPointerException Thrown if listener is null
     */
    void register(final ConnectionListener listener);

    /**
     * Unregisters a listener. If the listener is not registered, nothing happens
     * @param listener to be unregistered
     * @throws  NullPointerException Thrown if listener is null
     */
    void unregister(final ConnectionListener listener);
}
