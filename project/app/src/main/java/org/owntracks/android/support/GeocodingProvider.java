package org.owntracks.android.support;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.widget.TextView;

import org.owntracks.android.App;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.services.BackgroundService;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public interface GeocodingProvider {

    public void resolve(MessageLocation m, TextView tv);
    public void resolve(MessageLocation m, BackgroundService s);
}
