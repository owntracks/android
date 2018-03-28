package org.owntracks.android.robolectric;

import android.content.Intent;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.owntracks.android.R;
import org.owntracks.android.ui.status.StatusActivity;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O_MR1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = LOLLIPOP, maxSdk = O_MR1)
public class StatusActivityTest {

    private StatusActivity statusActivity;

    @Before
    public void setup() {
        statusActivity = Robolectric.setupActivity(StatusActivity.class);
        assertNotNull(statusActivity);
    }

    @Test
    @Config(minSdk = M)
    public void DozeWhiteListButtonShouldFireSettingsIntentOn() {
        statusActivity.findViewById(R.id.dozeWhiteListed).performClick();
        Intent expectedIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        Intent actualIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }


}
