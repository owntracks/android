package org.owntracks.android.e2e

import android.Manifest
import android.view.View
import android.view.animation.Animation
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.*
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class ContactActivityTests : TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    MockDeviceLocation by GPSMockDeviceLocation(),
    TestWithAnHTTPServer by TestWithAnHTTPServerImpl() {

    @Before
    fun setIdlingTimeout() {
        IdlingPolicies.setIdlingResourceTimeout(30, TimeUnit.SECONDS)
    }

    @Before
    fun startMockWebserver() {
        startServer(mapOf("/" to locationResponse))
    }

    @After
    fun stopMockWebserver() {
        stopServer()
    }

    @After
    fun uninitMockLocation() {
        unInitializeMockLocationProvider()
    }

    private val locationResponse = """
        {"_type":"location","acc":20,"al":0,"batt":100,"bs":0,"conn":"w","created_at":1610748273,"lat":51.2,"lon":-4,"tid":"aa","tst":1610799026,"vac":40,"vel":7}
    """.trimIndent()

    @Test
    fun testClickingOnContactLoadsContactOnMap() {
        setNotFirstStartPreferences()
        baristaRule.launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        initializeMockLocationProvider(InstrumentationRegistry.getInstrumentation().targetContext)

        configureHTTPConnectionToLocal()

        openDrawer()
        clickOnAndWait(R.string.title_activity_map)

        baristaRule.activityTestRule.activity.locationIdlingResource.with {
            waitUntilActivityVisible<MapActivity>()
            setMockLocation(51.0, 0.0)
            clickOnAndWait(R.id.menu_mylocation)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.with {
            openDrawer()
            clickOnAndWait(R.string.title_activity_contacts)
            assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
        }

        clickOnAndWait("aa")

        assertDisplayed(R.id.bottomSheetLayout)
        assertDisplayed(R.id.contactPeek)
        assertContains(R.id.name, "aa")

        clickOnAndWait(R.id.menu_mylocation)

        assertNotDisplayed(R.id.bottomSheetLayout)
        assertNotDisplayed(R.id.contactPeek)
    }

    class AnimationIdlingResource(view: View) : IdlingResource {
        private var callback: IdlingResource.ResourceCallback? = null
        override fun getName(): String {
            return AnimationIdlingResource::class.java.name
        }

        override fun isIdleNow(): Boolean {
            return true
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
            this.callback = callback
        }

        init {
            if (view.animation == null) {
                callback!!.onTransitionToIdle()
            } else {
                view.animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        callback!!.onTransitionToIdle()
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
        }
    }
}