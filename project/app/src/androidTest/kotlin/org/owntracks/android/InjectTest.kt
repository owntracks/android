package org.owntracks.android

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.AndroidJUnitRunner
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.CustomTestApplication
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.di.LocationProviderClientModule
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.testutils.JustThisTestPlease
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.map.MapActivity

class CustomTestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
    return super.newApplication(cl, TestApp_Application::class.java.name, context)
  }
}

@CustomTestApplication(BaseApp::class) interface TestApp

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@JustThisTestPlease
class InjectTest : TestWithAnActivity<MapActivity>(MapActivity::class.java, false) {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Before
  fun setup() {
    // First initialize Hilt
    hiltRule.inject()
  }

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

@TestInstallIn(
    components = [SingletonComponent::class], replaces = [LocationProviderClientModule::class])
@Module
class TestLocationProviderClientModule {
  @Provides
  @Singleton
  fun getLocationProviderClient(
      @Suppress("UNUSED_PARAMETER") @ApplicationContext applicationContext: Context
  ): LocationProviderClient = MockLocationProviderClient()
}
