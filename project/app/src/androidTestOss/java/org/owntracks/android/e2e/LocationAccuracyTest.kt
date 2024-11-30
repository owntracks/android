package org.owntracks.android.e2e

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlin.random.Random
import mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.di.LocationProviderClientModule
import org.owntracks.android.location.AospLocationProviderClient
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.reportLocationFromMap
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.waitUntilActivityVisible
import org.owntracks.android.ui.map.MapActivity
import javax.inject.Singleton

class CustomTestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
    return super.newApplication(cl, HiltTestApplication::class.java.name, context)
  }
}


@TestInstallIn(components = [SingletonComponent::class], replaces = [LocationProviderClientModule::class])
@Module
class TestLocationProviderClientModule {
  @Provides
  @Singleton
  fun getLocationProviderClient(
    @ApplicationContext applicationContext: Context
  ): LocationProviderClient = MockLocationProvderClient()
}


class MockLocationProvderClient : LocationProviderClient() {
  override fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper) {
    TODO("Not yet implemented")
  }

  override fun actuallyRequestLocationUpdates(
    locationRequest: LocationRequest,
    clientCallBack: LocationCallback,
    looper: Looper
  ) {
    TODO("Not yet implemented")
  }

  override fun removeLocationUpdates(clientCallBack: LocationCallback) {
    TODO("Not yet implemented")
  }

  override fun flushLocations() {
    TODO("Not yet implemented")
  }

  override fun getLastLocation(): Location? {
    TODO("Not yet implemented")
  }
}

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LocationAccuracyTest :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {

      @get:Rule
      var hiltRule = HiltAndroidRule(this)

//  @BindValue
//  @JvmField
//  val mockLocationProviderClient: LocationProviderClient = MockLocationProvderClient()

  @Test
  fun given_an_inaccurate_and_accurate_location_when_publishing_then_only_the_location_only_the_accurate_location_is_published() {
    val inaccurateMockLatitude = 22.0
    val inaccurateMockLongitude = 4.0
    val accurateMockLatitude = 22.123456
    val accurateMockLongitude = 4.123456

    PreferenceManager.getDefaultSharedPreferences(
        InstrumentationRegistry.getInstrumentation().targetContext,
    )
        .edit()
        .putInt(Preferences::ignoreInaccurateLocations.name, 50)
        .apply()

    setup()

    reportLocationFromMap(app.mockLocationIdlingResource) {
      setMockLocation(inaccurateMockLatitude, inaccurateMockLongitude, 3000f)
    }

    app.mockLocationIdlingResource.setIdleState(false)
    reportLocationFromMap(app.mockLocationIdlingResource) {
      setMockLocation(accurateMockLatitude, accurateMockLongitude)
    }

    mqttPacketsReceived
        .filterIsInstance<MQTTPublish>()
        .map { Pair(it.topicName, Parser(null).fromJson((it.payload)!!.toByteArray())) }
        .run {
          assertTrue(
              "received packets contains accurate location",
              any {
                it.second.run {
                  this is MessageLocation &&
                      latitude == accurateMockLatitude &&
                      longitude == accurateMockLongitude
                }
              },
          )
          assertFalse(
              "received packets doesn't contain inaccurate location",
              any {
                it.second.run {
                  this is MessageLocation &&
                      latitude == inaccurateMockLatitude &&
                      longitude == inaccurateMockLongitude
                }
              },
          )
        }
  }

  private fun setup() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    initializeMockLocationProvider(app)
    configureMQTTConnectionToLocalWithGeneratedPassword()
    waitUntilActivityVisible<MapActivity>()
  }
}
