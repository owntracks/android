package st.alr.mqttitude;

import android.app.Application;

import com.bugsnag.android.Bugsnag;

import st.alr.mqttitude.support.Defaults;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Bugsnag.register(this, Defaults.BUGSNAG_API_KEY);
        Bugsnag.setNotifyReleaseStages("production", "testing");
    }
}
