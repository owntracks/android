package org.owntracks.android.testutils

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep

interface MockDeviceLocation {
  fun initializeMockLocationProvider(context: Context)

  fun setMockLocation(latitude: Double, longitude: Double, accuracy: Float = 5f)

  fun setPackageAsMockLocationProvider(context: Context) {
    InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand("appops set ${context.packageName} android:mock_location allow")
        .use { it.dumpOutputToLog("setPackageMockLocationProvider") }
    sleep(100)
  }
}
