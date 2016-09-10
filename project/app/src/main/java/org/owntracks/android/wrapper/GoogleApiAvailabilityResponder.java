package org.owntracks.android.wrapper;

import android.content.Context;

public interface GoogleApiAvailabilityResponder {
    Context getContext();

    void onPlayServicesAvailable();

    void onPlayServicesUnavailableRecoverable(int resultCode);

    void onPlayServicesUnavailableNotRecoverable(int resultCode);
}
