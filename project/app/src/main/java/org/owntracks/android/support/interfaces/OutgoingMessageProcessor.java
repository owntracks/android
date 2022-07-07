package org.owntracks.android.support.interfaces;


public interface OutgoingMessageProcessor {
    void onCreateFromProcessor();
    void onDestroy();

    void checkConfigurationComplete() throws ConfigurationIncompleteException;
}
