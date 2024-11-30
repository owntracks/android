package org.owntracks.android.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnHTTPServer
import org.owntracks.android.testutils.TestWithAnHTTPServerImpl
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.map.MapActivity

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ContactsAndMapTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnHTTPServer by TestWithAnHTTPServerImpl() {

  private val locationResponse =
      """
        {"_type":"location","acc":20,"al":0,"batt":100,"bs":0,"conn":"w","created_at":1610748273,"lat":51.2,"lon":-4,"tid":"aa","tst":1610799026,"vac":40,"vel":7}
    """
          .trimIndent()

  @Test
  fun test_clicking_on_contact_loads_contact_on_map() {
    startServer(mapOf("/" to locationResponse))
    setNotFirstStartPreferences()
    launchActivity()

    grantMapActivityPermissions()
    configureHTTPConnectionToLocal(saveConfigurationIdlingResource)
    waitUntilActivityVisible()
    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(51.0, 0.0)
    }

    baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
      openDrawer()
      clickOn(R.string.title_activity_contacts)
    }

    assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)

    clickOn("aa")
    waitUntilActivityVisible(MapActivity::class.java)

    assertDisplayed(R.id.bottomSheetLayout)
    assertDisplayed(R.id.contactPeek)
    assertContains(R.id.name, "aa")

    clickBack()

    assertNotDisplayed(R.id.bottomSheetLayout)
    assertNotDisplayed(R.id.contactPeek)
  }
}
