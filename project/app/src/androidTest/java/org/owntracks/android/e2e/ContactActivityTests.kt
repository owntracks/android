package org.owntracks.android.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickBack
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ui.contacts.ContactsActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class ContactActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(ContactsActivity::class.java)

    private var mockWebServer = MockWebServer()

    @After
    fun stopMockWebserver() {
        mockWebServer.shutdown()
    }

    private val locationResponse = """
        {"_type":"location","acc":20,"al":0,"batt":100,"bs":0,"conn":"w","created_at":1610748273,"lat":51.2,"lon":-4,"tid":"aa","tst":1610799026,"vac":40,"vel":7}
    """.trimIndent()

    @Test
    @AllowFlaky(attempts = 1)
    fun testClickingOnContactLoadsContactOnMap() {
        mockWebServer.start()
        mockWebServer.dispatcher = MockWebserverLocationDispatcher(locationResponse)
        val httpPort = mockWebServer.port
        baristaRule.launchActivity()
        openDrawer()
        clickOn(R.string.title_activity_preferences)
        clickOn(R.string.preferencesServer)
        clickOn(R.string.mode_heading)
        clickOn(R.string.mode_http_private_label)
        clickDialogPositiveButton()
        clickOn(R.string.preferencesHost)
        writeTo(R.id.url, "http://localhost:${httpPort}/")
        clickDialogPositiveButton()
        clickBack()

        openDrawer()
        clickOn(R.string.title_activity_map)
        clickOn(R.id.btn_next)
/* TODO Once test isolation is possible we'll have to grant the priv each test */
//        clickOn(R.id.btn_next)
//        clickOn(R.id.fix_permissions_button)
//        LocationPermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        clickOn(R.id.btn_next)
        clickOn(R.id.done)
        clickOn(R.id.menu_report)
        openDrawer()
        clickOn(R.string.title_activity_contacts)
        sleep(5000) // Wait for the HTTP req/response
        assertRecyclerViewItemCount(R.id.recycler_view, 1)

        clickOn("aa")
        assertDisplayed(R.id.bottomSheetLayout)
        assertDisplayed(R.id.contactPeek)
        assertContains(R.id.name,"aa")
    }

    class MockWebserverLocationDispatcher(private val config: String) : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val errorResponse = MockResponse().setResponseCode(404)
            return if (request.path == "/") {
                MockResponse().setResponseCode(200).setHeader("Content-type", "application/json").setBody(config)
            } else {
                errorResponse
            }
        }
    }
}