package org.owntracks.android.support;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.owntracks.android.App;
import org.owntracks.android.BuildConfig;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class StatisticsProvider  {
    public static final String REFERENCE = "REFERENCE";
    public static final String APP_START = "APP_START";
    public static final String SERVICE_PROXY_START = "SERVICE_PROXY_START";
    public static final String SERVICE_LOCATOR_PLAY_CONNECTED = "SERVICE_LOCATOR_PLAY_CONNECTED";
    public static final String SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE = "SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE";
    public static final String SERVICE_LOCATOR_BACKGROUND_LOCATION_CHANGES = "SERVICE_LOCATOR_BACKGROUND_LOCATION_CHANGES";
    public static final String SERVICE_BROKER_LOCATION_PUBLISH_INIT = "SERVICE_BROKER_LOCATION_PUBLISH_INIT";
    public static final String SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS0_DROP = "SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS0_DROP";
    public static final String SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS12_QUEUE = "SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS12_QUEUE";
    public static final String SERVICE_BROKER_LOCATION_PUBLISH_SUCCESS = "SERVICE_BROKER_LOCATION_PUBLISH_SUCCESS";
    public static final String SERVICE_BROKER_QUEUE_LENGTH = "SERVICE_BROKER_QUEUE_LENGTH";
    public static final String SERVICE_BROKER_CONNECTS = "SERVICE_BROKER_CONNECTS";

    private static InternalProviderInterface provider = new Provider();


    public static void setInt(String key, int value) {
        provider.setInt(key, value);
    }
    public static int getInt(String key) {
        return provider.getInt(key);
    }

    public static void setTime(String key) {
        provider.setTime(key);
    }

    public static Date getTime(String key) {
        return provider.getTime(key);
    }

    public static void initialize(App app) {
        provider.setTime(StatisticsProvider.APP_START);
    }


    private interface InternalProviderInterface {
        void setInt( String key, int value);
        Integer getInt(String key);
        void setTime(String key);
        Date getTime(String key);

    }

    public static class Provider implements InternalProviderInterface {
        private Map<String, Object> counter = new HashMap<>();

        public void setInt(String key, int value) {
            counter.put(key, value);
        }

        @NonNull
        public Integer getInt(String key) {
            return counter.containsKey(key) ? (Integer) counter.get(key) : 0;
        }

        public void setTime(String key) {
            counter.put(key, new Date());
        }

        @NonNull
        public Date getTime(String key) {
            return counter.containsKey(key) ? (Date)counter.get(key) : new Date(0);
        }

    }
}
