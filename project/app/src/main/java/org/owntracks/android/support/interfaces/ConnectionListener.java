package org.owntracks.android.support.interfaces;

/**
 * Interface for connection status changes
 */

public interface ConnectionListener {
    /** Called when a connection is established */
    public void onConnected();
    /** Called when the connection is closed */
    public void onClosed();

    /**
     * This method is called, when the connection raised an error
     * @param t Error that has been raised by the connection
     */
    public void onError(final Throwable t);
}
