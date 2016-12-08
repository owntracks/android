package org.owntracks.android.support;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.widget.TextView;

import org.owntracks.android.App;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.services.ServiceNotification;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class GeocodingProvider {
    private static final Double RUN_FIRST = 1d;
    private static final Double RUN_SECOND = 2d;
    private static Geocoder geocoder;

    public static void resolve(MessageLocation m, TextView tv) {
        if(m.hasGeocoder()) {
            tv.setText(m.getGeocoder());
        } else {
            tv.setText(m.getGeocoderFallback()); // will print lat : lon until Geocoder is available
            TextViewLocationResolverTask.run(m, tv, RUN_FIRST);
        }
    }

    public static void resolve(MessageLocation m, ServiceNotification s) {
        NotificationLocationResolverTask.run(m, s, RUN_FIRST);
    }

    private static class NotificationLocationResolverTask extends MessageLocationResolverTask {

        private final WeakReference<ServiceNotification> service;

        static void run(MessageLocation m, ServiceNotification s, double run) {
            (new NotificationLocationResolverTask(m, s)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m.getLatitude(), m.getLongitude(), run);
        }

        @Override
        void retry() {
            Timber.v("retrying");
            MessageLocation m = message.get();
            ServiceNotification s = service.get();

            if(m != null && s != null)
                run(m, s, RUN_SECOND);
        }

        NotificationLocationResolverTask(MessageLocation m, ServiceNotification service) {
            super(m);
            this.service = new WeakReference<>(service);

        }

        @Override
        void onPostExecuteResultAvailable(String result) {
            MessageLocation m = this.message.get();
            ServiceNotification s = this.service.get();
            if(m!=null && s!=null) {
                s.onMessageLocationGeocoderResult(m);
            }
        }


    }

    private static class TextViewLocationResolverTask extends MessageLocationResolverTask {

        private final WeakReference<TextView> textView;

        static void run(MessageLocation m, TextView tv, double run) {
            (new TextViewLocationResolverTask(m, tv)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m.getLatitude(), m.getLongitude(), run);
        }

        @Override
        void retry() {
            MessageLocation m = message.get();
            TextView tv = textView.get();

            if(m != null && tv != null)
                run(m, tv, RUN_SECOND);
        }



        TextViewLocationResolverTask(MessageLocation m, TextView tv) {
            super(m);
            this.textView = new WeakReference<>(tv);

        }

        @Override
        void onPostExecuteResultAvailable(String result) {
            Timber.v("TextViewLocationResolverTask run:%s result: %s", run, result);

            TextView s = this.textView.get();
            if(s!=null && result != null) {
                s.setText(result);
            }
        }
    }

    private static abstract class MessageLocationResolverTask extends AsyncTask<Double, Void, String>  {
        protected Double run;


        final WeakReference<MessageLocation> message;
        MessageLocationResolverTask(MessageLocation m) {
            this.message = new WeakReference<>(m);
        }

        @Override
        protected String doInBackground(Double... params) {
            run = params[2];

            if(!Geocoder.isPresent()) {
                Timber.e("geocoder is not present");
                return null;
            }

            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocation(params[0], params[1], 1);
                if ((addresses != null) && (addresses.size() > 0)) {
                    StringBuilder g = new StringBuilder();
                    Address a = addresses.get(0);

                    //String th = a.getThoroughfare();
                    //String sth = a.getSubThoroughfare();
                    //String lcy = a.getLocality();
                    //String co = a.getCountryCode();
                    //String ad0 = a.getAddressLine(0);
                    //if (ad0 != null && th != null && sth != null && lcy !=null && co != null && ad0.equalsIgnoreCase(lcy))
                    //{
                    //    g.append(th).append(" ").append(sth).append(", ").append(lcy).append(", ").append(co);
                    //    return g.toString();
                    //}
                    if (a.getAddressLine(0) != null)
                        g.append(a.getAddressLine(0)).append(", ");
                    if (a.getLocality() != null)
                        g.append(a.getLocality());
                    else if (a.getCountryName() != null)
                        g.append(a.getCountryName());
                    return g.toString();
                } else {
                    return "not available";
                }
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            MessageLocation m = message.get();
            Timber.v("result run:%s result:%s message:%s", run, result, m);

            // Retry once if request timed out or we didn't get a result for some temporary reason
            if(result == null && run.equals(RUN_FIRST) && m != null) {
                retry();
                return;
            }


            if(m!=null && result != null) {
                m.setGeocoder(result);
                onPostExecuteResultAvailable(result);
            }
        }

        abstract void onPostExecuteResultAvailable(String result);
        abstract void retry();

    }

    public static void initialize(Context c){
        geocoder = new Geocoder(App.getContext(), Locale.getDefault());
    }



}
