package st.alr.mqttitude;

import android.app.Application;
import android.content.Context;

import com.bugsnag.android.Bugsnag;

import st.alr.mqttitude.support.Defaults;


public class App extends Application {
    private static App instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Bugsnag.register(this, Defaults.BUGSNAG_API_KEY);
        Bugsnag.setNotifyReleaseStages("production", "testing");
    }

    public static Context getContext() {
        return instance;
    }

}
