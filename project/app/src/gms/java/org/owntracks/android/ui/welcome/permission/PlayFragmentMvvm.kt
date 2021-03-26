package org.owntracks.android.ui.welcome.permission

import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm

interface PlayFragmentMvvm {
    interface View : WelcomeFragmentMvvm.View {
        fun requestFix()
    }
}