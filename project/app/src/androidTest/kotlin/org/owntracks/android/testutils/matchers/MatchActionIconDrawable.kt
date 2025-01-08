package org.owntracks.android.testutils.matchers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Espresso matcher for an action menu icon and checking that it has the given drawable
 *
 * @param resourceId of the drawable displayed by the action menu item
 * @return an espresso matcher
 */
// https://stackoverflow.com/a/34063950/352740
fun withActionIconDrawable(@DrawableRes resourceId: Int): Matcher<View?> {
  return object : BoundedMatcher<View?, ActionMenuItemView>(ActionMenuItemView::class.java) {
    override fun describeTo(description: Description) {
      description.appendText("has image drawable resource $resourceId")
    }

    override fun matchesSafely(actionMenuItemView: ActionMenuItemView): Boolean {
      return sameBitmap(
          actionMenuItemView.context,
          actionMenuItemView.itemData.icon!!,
          resourceId,
          actionMenuItemView)
    }
  }
}

private fun sameBitmap(context: Context, drawable: Drawable, resourceId: Int, view: View): Boolean {
  val otherDrawable: Drawable = context.resources.getDrawable(resourceId, null) ?: return false

  val actualDrawable =
      when (drawable) {
        is StateListDrawable -> {
          val getStateDrawableIndex =
              StateListDrawable::class.java.getMethod("getStateDrawableIndex", IntArray::class.java)
          val getStateDrawable =
              StateListDrawable::class
                  .java
                  .getMethod("getStateDrawable", Int::class.javaPrimitiveType)
          getStateDrawable.invoke(
              drawable, getStateDrawableIndex.invoke(drawable, view.drawableState)) as Drawable
        }
        else -> drawable
      }
  val bitmap = getBitmapFromDrawable(actualDrawable)
  val otherBitmap = getBitmapFromDrawable(otherDrawable)
  return bitmap.sameAs(otherBitmap)
}

private fun getBitmapFromDrawable(drawable: Drawable): Bitmap =
    Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        .apply {
          val canvas = Canvas(this)
          drawable.setBounds(0, 0, canvas.width, canvas.height)
          drawable.draw(canvas)
        }
