package org.owntracks.android

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.map.MapActivity

@HiltAndroidTest
class InjectTest : TestWithAnActivity<MapActivity>(false) {

  @Test
  fun hiltInjectTest() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    mockLocationProviderClient.setLocation(51.0, 0.0)
    assertDisplayed(R.id.menu_monitoring)
  }
}
