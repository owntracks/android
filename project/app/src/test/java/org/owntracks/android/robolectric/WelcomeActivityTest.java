package org.owntracks.android.robolectric;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.owntracks.android.R;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.welcome.WelcomeActivity;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.O_MR1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = LOLLIPOP, maxSdk = O_MR1)
public class WelcomeActivityTest {
    private WelcomeActivity welcomeActivity;

    @Before
    public void setup() {
        welcomeActivity = Robolectric.setupActivity(WelcomeActivity.class);
        assertNotNull(welcomeActivity);

    }

    @Test
    public void DoneButtonShouldStartMapActivity() {
        welcomeActivity.findViewById(R.id.done).performClick();
        Intent expectedIntent = new Intent(welcomeActivity, MapActivity.class);
        Intent actualIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
