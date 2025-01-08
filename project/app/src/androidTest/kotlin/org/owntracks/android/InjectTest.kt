package org.owntracks.android

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.CustomTestApplication
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.testutils.JustThisTestPlease
import org.owntracks.android.testutils.TestWithAnActivity
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
class InjectTest: TestWithAnActivity<MapActivity>(MapActivity::class.java,true) {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Before
  fun setup() {
    // First initialize Hilt
    hiltRule.inject()
  }

  @Test
  fun hiltInjectTest() {
    // ...
  }
}
