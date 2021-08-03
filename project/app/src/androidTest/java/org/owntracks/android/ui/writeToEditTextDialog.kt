package org.owntracks.android.ui

import android.R
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions

fun writeToEditTextDialog(@StringRes name: Int, value: String) {
    scrollToText(name)
    clickOnAndWait(name)
    BaristaEditTextInteractions.writeTo(R.id.edit, value)
    BaristaDialogInteractions.clickDialogPositiveButton()
}

fun scrollToText(textResource: Int) {
    Espresso.onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(ViewMatchers.withText(textResource)),
                ViewActions.scrollTo()
            )
        )
}