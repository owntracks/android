package org.owntracks.android.services;

import org.owntracks.android.support.interfaces.ConnectionListener;
import org.owntracks.android.support.interfaces.StatefulServiceMessageEndpoint;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by phoenix on 15.05.17.
 */

public abstract class ServiceMessageProvider implements StatefulServiceMessageEndpoint {

    /** Attached connection listenery */
    private final List<ConnectionListener> connectionListeners = new LinkedList<>();

    /** Observable for all attached connection listeners.
     * Calling this methods will invoke all attached ConnectionListeners on the same method.
     * Use this object to notify all observing #ConnectionListener instances
      */
    protected final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onConnected() {
            synchronized (connectionListeners) {
                for(final ConnectionListener l : connectionListeners)
                    l.onConnected();
            }
        }

        @Override
        public void onClosed() {
            synchronized (connectionListeners) {
                for(final ConnectionListener l : connectionListeners)
                    l.onClosed();
            }
        }

        @Override
        public void onError(Throwable t) {
            synchronized (connectionListeners) {
                for(final ConnectionListener l : connectionListeners)
                    l.onError(t);
            }
        }
    };

    /**
     * Registers a connection listener. The same listener can only be added once, if executed multiple times on the same listener, nothing happens
     * @param listener to be added
     * @throws  NullPointerException Thrown if listener is null
     */
    public void register(final ConnectionListener listener) {
        if(listener == null) throw new NullPointerException();
        synchronized (this.connectionListeners) {
            if(this.connectionListeners.contains(listener)) return;
            else
                this.connectionListeners.add(listener);
        }
    }

    /**
     * Unregisters a listener. If the listener is not registered, nothing happens
     * @param listener to be unregistered
     * @throws  NullPointerException Thrown if listener is null
     */
    public void unregister(final ConnectionListener listener) {
        if(listener == null) throw new NullPointerException();
        synchronized (this.connectionListeners) {
            this.connectionListeners.remove(listener);
        }
    }



}
