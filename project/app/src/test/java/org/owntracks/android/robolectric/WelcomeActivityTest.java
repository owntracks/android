package org.owntracks.android.robolectric;

import android.Manifest;
import android.content.Intent;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.welcome.WelcomeActivity;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.gms.common.ShadowGoogleApiAvailability;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.shadows.ShadowView.clickOn;

/**
 * Objectbox doesn't like being used with Robolectric due to trying to repeatedly load the native
 * lib and not failing gracefully if it's already loaded. Thus, we have a new App that swaps out the
 * ObjectboxWaypointsRepo and uses a component that injects a DummyWaypointsRepo instead.
 */
class TestApp extends App {
    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        AppComponentForTest appComponent = DaggerAppComponentForTest.builder().app(this).build();
        appComponent.inject(this);
        return appComponent;
    }
}


@RunWith(RobolectricTestRunner.class)
@Config(minSdk = LOLLIPOP, maxSdk = P, application = TestApp.class, shadows = {ShadowViewPager.class, ShadowGoogleApiAvailability.class})
public class WelcomeActivityTest {

    @Spy
    private WelcomeActivity welcomeActivity;

    private ShadowApplication application;


    @Before
    public void setup() {
        final ShadowGoogleApiAvailability shadowGoogleApiAvailability
                = Shadow.extract(GoogleApiAvailability.getInstance());
        shadowGoogleApiAvailability.setIsGooglePlayServicesAvailable(ConnectionResult.SUCCESS);

        ActivityController<WelcomeActivity> welcomeActivityActivityController = Robolectric.buildActivity(WelcomeActivity.class);


        welcomeActivity = welcomeActivityActivityController.get();
        application = Shadows.shadowOf(welcomeActivity.getApplication());

        application.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        assertNotNull(welcomeActivity);
        welcomeActivityActivityController.setup();

    }

    @Test
    @Config(minSdk = M)
    public void DoneButtonShouldStartMapActivityOnVersionWithBackgroundRestriction() {
        assertEquals(View.VISIBLE, welcomeActivity.findViewById(R.id.btn_next).getVisibility());
        clickOn(welcomeActivity.findViewById(R.id.btn_next));

        assertEquals(View.VISIBLE, welcomeActivity.findViewById(R.id.btn_next).getVisibility());
        clickOn(welcomeActivity.findViewById(R.id.btn_next));

        assertEquals(View.VISIBLE, welcomeActivity.findViewById(R.id.done).getVisibility());

        assertTrue(welcomeActivity.findViewById(R.id.done).isEnabled());

        //Pager is at the end. Next button should be hidden
        assertEquals(View.GONE, welcomeActivity.findViewById(R.id.btn_next).getVisibility());

        clickOn(welcomeActivity.findViewById(R.id.done));
        Intent expectedIntent = new Intent(welcomeActivity, MapActivity.class);
        Intent actualIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    @Config(maxSdk = LOLLIPOP_MR1)
    public void DoneButtonShouldStartMapActivityOnVersionWithoutBackgroundRestriction() {
        assertEquals(View.VISIBLE, welcomeActivity.findViewById(R.id.btn_next).getVisibility());
        clickOn(welcomeActivity.findViewById(R.id.btn_next));

        assertEquals(View.VISIBLE, welcomeActivity.findViewById(R.id.done).getVisibility());
        assertTrue(welcomeActivity.findViewById(R.id.done).isEnabled());

        //Pager is at the end. Next button should be hidden
        assertEquals(View.GONE, welcomeActivity.findViewById(R.id.btn_next).getVisibility());

        clickOn(welcomeActivity.findViewById(R.id.done));
        Intent expectedIntent = new Intent(welcomeActivity, MapActivity.class);
        Intent actualIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}