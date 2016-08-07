package org.owntracks.android.support;

import android.support.annotation.NonNull;

import org.owntracks.android.App;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class StatisticsProvider  {
    public static final String APP_START = "APP_START";
    public static final String SERVICE_PROXY_START = "SERVICE_PROXY_START";
    public static final String SERVICE_LOCATOR_PLAY_CONNECTED = "SERVICE_LOCATOR_PLAY_CONNECTED";
    public static final String SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE = "SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE";
    public static final String SERVICE_MESSAGE_QUEUE_LENGTH = "SERVICE_MESSAGE_QUEUE_LENGTH";
    public static final String SERVICE_MESSAGE_BACKEND_LAST_STATUS = "SERVICE_MESSAGE_BACKEND_LAST_STATUS";
    public static final String SERVICE_MESSAGE_BACKEND_LAST_STATUS_TST = "SERVICE_MESSAGE_BACKEND_LAST_STATUS_TST";

    private static InternalProviderInterface provider = new Provider();
    public static void setString(String key, String value) {
        provider.setString(key, value);
    }
    public static String getString(String key) {
        return provider.getString(key);
    }


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
        void setString(String key, String value);
        String getString(String key);
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
        public void setString(String key, String value) {
            counter.put(key, value);
        }

        @NonNull
        public String getString(String key) {
            return counter.containsKey(key) ? (String) counter.get(key) : "";
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
