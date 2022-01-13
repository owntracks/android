package org.owntracks.android.testutils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import org.owntracks.android.R
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber
import java.util.concurrent.TimeUnit

fun scrollToPreferenceWithText(textResource: Int) {
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(textResource)),
                ViewActions.scrollTo()
            )
        )
}

fun setNotFirstStartPreferences() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
        .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
        .apply()
}

fun reportLocationFromMap(locationIdlingResource: IdlingResource?) {
    openDrawer()
    clickOnAndWait(R.string.title_activity_map)
    Timber.d("Waiting for location")
    locationIdlingResource.with {
        waitUntilActivityVisible<MapActivity>()
        clickOnAndWait(R.id.menu_mylocation)
    }
    Timber.d("location now available")
    clickOnAndWait(R.id.menu_report)
}

const val TIMEOUT = 5000L
const val CONDITION_CHECK_INTERVAL = 100L

inline fun <reified T : Activity> waitUntilActivityVisible() {
    val startTime = System.currentTimeMillis()
    Timber.d("Waiting for ${T::class.java.simpleName} to be visible")
    while (!isVisible<T>()) {
        Thread.sleep(CONDITION_CHECK_INTERVAL)
        if (System.currentTimeMillis() - startTime >= TIMEOUT) {
            throw AssertionError("Activity ${T::class.java.simpleName} not visible after $TIMEOUT milliseconds")
        }
    }
    Timber.d("${T::class.java.simpleName} is now visible visible")
}

inline fun <reified T : Activity> isVisible(): Boolean {
    val am =
        InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val visibleActivityName = am.appTasks[0].taskInfo.baseActivity?.className
    Timber.d("Visible activity is $visibleActivityName. Looking for ${T::class.java.name}")
    return visibleActivityName == T::class.java.name
}

inline fun IdlingResource?.with(timeoutSeconds: Long = 30, block: () -> Unit) {
    if (this == null) {
        Timber.w("Idling resource is null")
    }
    IdlingPolicies.setIdlingResourceTimeout(timeoutSeconds, TimeUnit.SECONDS)
    try {
        this?.run { IdlingRegistry.getInstance().register(this) }
        block()
    } finally {
        this?.run { IdlingRegistry.getInstance().unregister(this) }
    }
}