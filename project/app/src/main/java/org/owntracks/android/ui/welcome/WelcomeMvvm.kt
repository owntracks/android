package org.owntracks.android.ui.welcome

import org.owntracks.android.ui.base.view.MvvmView

interface WelcomeMvvm {
    // The view methods get called from all sorts of places. Fragments etc.
    interface View : MvvmView {
        fun setPagerIndicator(position: Int)
    }
}