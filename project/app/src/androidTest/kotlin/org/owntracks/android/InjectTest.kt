package org.owntracks.android

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setLocation
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.map.MapActivity

@HiltAndroidTest
class InjectTest : TestWithAnActivity<MapActivity>(MapActivity::class.java, false) {

  @Inject lateinit var mockLocationProviderClient: LocationProviderClient

  @Test
  fun hiltInjectTest() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    mockLocationProviderClient.setLocation(51.0, 0.0)
    assertDisplayed(R.id.menu_monitoring)
  }
}
