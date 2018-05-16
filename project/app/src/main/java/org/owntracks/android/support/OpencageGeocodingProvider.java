package org.owntracks.android.support;

import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.v4.util.LruCache;
import android.widget.TextView;

import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.services.BackgroundService;

import java.lang.ref.WeakReference;
import java.util.Locale;

import timber.log.Timber;

public class OpencageGeocodingProvider implements GeocodingProvider {

    private static LruCache<String, String> cache;
    static OpencageGeocoder jOpenCageGeocoder;
    Preferences preferences;

    public OpencageGeocodingProvider(Preferences preferences) {
        cache = new LruCache<String, String>(20);
        this.preferences = preferences;
        jOpenCageGeocoder = new OpencageGeocoder(preferences.getOpenCageGeocoderApiKey());
    }

    private static String getCache(MessageLocation m) {
        return cache.get(locationHash(m));
    }

    static void putCache(MessageLocation m, String geocoder) {
        cache.put(locationHash(m), geocoder);
    }

    private static String locationHash(MessageLocation m) {
        return String.format(Locale.US,"%.6f-%.6f", m.getLatitude(), m.getLongitude());
    }

    private boolean isCachedGeocoderAvailable(MessageLocation m) {
        String s = getCache(m);
        Timber.v("cache lookup for %s (hash %s) -> %s", m.getMessageId(), locationHash(m), getCache(m));

        if(s != null) {
            m.setGeocoder(s);
            return true;
        } else {
            return false;
        }
    }
    @Override
    public void resolve(MessageLocation m, TextView tv) {
        if(m.hasGeocoder()) {
            tv.setText(m.getGeocoder());
            return;
        }

        if(isCachedGeocoderAvailable(m)) {
            tv.setText(m.getGeocoder());
        } else {
            tv.setText(m.getGeocoderFallback()); // will print lat : lon until Geocoder is available
            TextViewLocationResolverTask.run(m, tv);
        }
    }

    @Override
    public void resolve(MessageLocation m, BackgroundService s) {
        if(m.hasGeocoder()) {
            s.onGeocodingProviderResult(m);
            return;
        }

        if(isCachedGeocoderAvailable(m)) {
            s.onGeocodingProviderResult(m);
        } else {
            NotificationLocationResolverTask.run(m, s);
        }
    }


    private static class NotificationLocationResolverTask extends OpencageGeocodingProvider.MessageLocationResolverTask {

        private final WeakReference<BackgroundService> service;

        static void run(MessageLocation m, BackgroundService s) {
            (new NotificationLocationResolverTask(m, s)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        NotificationLocationResolverTask(MessageLocation m, BackgroundService service) {
            super(m);
            this.service = new WeakReference<>(service);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            MessageLocation m = this.message.get();
            BackgroundService s = this.service.get();
            if(m!=null && s!=null) {
                s.onGeocodingProviderResult(m);
            }
        }
    }

    private static class TextViewLocationResolverTask extends OpencageGeocodingProvider.MessageLocationResolverTask {

        private final WeakReference<TextView> textView;

        static void run(MessageLocation m, TextView tv) {
            (new TextViewLocationResolverTask(m, tv)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        TextViewLocationResolverTask(MessageLocation m, TextView tv) {
            super(m);
            this.textView = new WeakReference<>(tv);

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            TextView s = this.textView.get();
            if(s!=null && result != null) {
                s.setText(result);
            }
        }
    }

    private static abstract class MessageLocationResolverTask extends AsyncTask<Void, Void, String>  {
        final WeakReference<MessageLocation> message;
        MessageLocationResolverTask(MessageLocation m) {
            this.message = new WeakReference<>(m);
        }

        @Override
        protected String doInBackground(Void... params) {
// In real live application the JOpenCageGeocoder should be a Singleton
            MessageLocation m = message.get();
            if(m == null) {
                return "Resolve failed";
            }

            //JOpenCageResponse response = jOpenCageGeocoder.reverse(m.getLatitude(), m.getLongitude());

           // JOpenCageComponents c = response.getFirstComponents();

            //response.
            //StringBuilder b = new StringBuilder();
            // b.append(c.getRoad()).append(",");
            //if(c.getHouseNumber() != null)
            //    b.append(c.getHouseNumber()).append(",");
            //return response.getResults().get(0).getFormatted();// b.append(c.getCity()).toString();
            return jOpenCageGeocoder.reverse(m.getLatitude(), m.getLongitude());
        }

        @Override
        @CallSuper
        protected void onPostExecute(String result) {
            MessageLocation m = message.get();
            Timber.v("geocoding result: %s", result);
            if(m!=null && result != null) {
                m.setGeocoder(result);
                putCache(m, result);
            }
        }
    }

}
