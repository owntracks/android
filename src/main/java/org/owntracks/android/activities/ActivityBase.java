package org.owntracks.android.activities;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.owntracks.android.App;

public class ActivityBase extends AppCompatActivity {
    private static final String TAG = "ActivityBase";
    private static int runningActivities = 0;
    public void onStart() {
        super.onStart();
        if (runningActivities == 0) {
            App.onEnterForeground();
        }
        runningActivities++;
    }

    public void onStop() {
        super.onStop();
        runningActivities--;
        if (runningActivities == 0) {
            App.onEnterBackground();
        }
    }


}
