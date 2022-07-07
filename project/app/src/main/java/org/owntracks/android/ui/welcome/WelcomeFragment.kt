package org.owntracks.android.ui.welcome

import android.content.Context
import androidx.fragment.app.Fragment

abstract class WelcomeFragment : Fragment() {
    abstract fun shouldBeDisplayed(context: Context): Boolean
}
