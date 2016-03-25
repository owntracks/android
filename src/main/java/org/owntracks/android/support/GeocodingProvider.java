package org.owntracks.android.support;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.services.ServiceNotification;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class GeocodingProvider {
    private static final String TAG = "GeocodingProvider";
    private static Context context;
    private static final Double RUN_FIRST = 1d;
    private static final Double RUN_SECOND = 2d;

    public static void resolve(MessageLocation m) {
        MessageLocationResolverTask.execute(m, RUN_FIRST);
    }

    public static void resolve(MessageLocation m, ServiceNotification s) {
        NotificationLocationResolverTask.execute(m, s, RUN_FIRST);
    }

    private static class NotificationLocationResolverTask extends MessageLocationResolverTask {

        private final WeakReference<ServiceNotification> service;

        public static void execute(MessageLocation m, ServiceNotification s, double run) {
            (new NotificationLocationResolverTask(m, s)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m.getLatitude(), m.getLongitude(), run);
        }


        public NotificationLocationResolverTask(MessageLocation m, ServiceNotification service) {
            super(m);
            this.service = new WeakReference<>(service);

        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            MessageLocation m = this.message.get();
            ServiceNotification s = this.service.get();
            if(m!=null && s!=null) {
                s.onMessageLocationGeocoderResult(m);
            }
        }
    }
    private static class MessageLocationResolverTask extends ResolverTask {
        public static void execute(MessageLocation m, double run) {
            (new MessageLocationResolverTask(m)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m.getLatitude(), m.getLongitude(), run);
        }

        final WeakReference<MessageLocation> message;
        public MessageLocationResolverTask(MessageLocation m) {
            this.message = new WeakReference<>(m);
        }

        @Override
        protected void onPostExecute(String result) {
            // Retry once if request timed out or we didn't get a result for some temporary reason
            if(result == null && run.equals(RUN_FIRST) && message.get() != null) {
                MessageLocationResolverTask.execute(message.get(), RUN_SECOND);
                return;
            }


            MessageLocation m = message.get();
            if(m!=null)
                m.setGeocoder(result);
        }
    }


    private abstract static class ResolverTask extends AsyncTask<Double, Void, String> {
        protected Double lat;
        protected Double lon;
        protected Double run;

        protected abstract void onPostExecute(String result);


        @Override
        protected String doInBackground(Double... params) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            lat = params[0];
            lon = params[1];
            run = params[2];

            if(!Geocoder.isPresent()) {
               Log.e(TAG, "geocoder is not present");
                return null;
            }

            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocation(lat, lon, 1);
                if ((addresses != null) && (addresses.size() > 0)) {
                    StringBuilder g = new StringBuilder();
                    if (addresses.get(0).getAddressLine(0) != null)
                        g.append(addresses.get(0).getAddressLine(0)).append(", ");
                    if (addresses.get(0).getLocality() != null)
                        g.append(addresses.get(0).getLocality());
                    else if (addresses.get(0).getCountryName() != null)
                        g.append(addresses.get(0).getCountryName());
                    return g.toString();
                } else {
                    return "not available";
                }
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static void initialize(Context c){
        context = c;
    }



}
