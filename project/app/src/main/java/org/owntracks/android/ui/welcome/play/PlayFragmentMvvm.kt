package org.owntracks.android.ui.welcome.play

import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm

interface PlayFragmentMvvm {
    interface View : WelcomeFragmentMvvm.View {
        fun requestFix()
    }
}