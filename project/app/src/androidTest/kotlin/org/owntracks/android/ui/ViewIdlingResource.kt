package org.owntracks.android.ui

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.ViewFinder
import java.lang.reflect.Field
import org.hamcrest.Matcher

// https://stackoverflow.com/a/60458739/352740
class ViewIdlingResource(
    private val viewMatcher: Matcher<View?>?,
    private val idleMatcher: Matcher<View?>?
) : IdlingResource {

  private var resourceCallback: IdlingResource.ResourceCallback? = null

  override fun isIdleNow(): Boolean {
    val view: View? = getView(viewMatcher)
    val isIdle: Boolean = idleMatcher?.matches(view) ?: false
    if (isIdle) {
      resourceCallback?.onTransitionToIdle()
    }
    return isIdle
  }

  override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback?) {
    this.resourceCallback = resourceCallback
  }

  override fun getName(): String {
    return "$this ${viewMatcher.toString()}"
  }

  private fun getView(viewMatcher: Matcher<View?>?): View? {
    return try {
      val viewInteraction = Espresso.onView(viewMatcher)
      val finderField: Field? = viewInteraction.javaClass.getDeclaredField("viewFinder")
      finderField?.isAccessible = true
      val finder = finderField?.get(viewInteraction) as ViewFinder
      finder.view
    } catch (e: Exception) {
      null
    }
  }
}
