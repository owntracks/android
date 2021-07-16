package org.owntracks.android.ui.welcome.intro

import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeIntroBinding
import org.owntracks.android.ui.welcome.BaseWelcomeFragment
import javax.inject.Inject

@AndroidEntryPoint
class IntroFragment @Inject constructor() :
        BaseWelcomeFragment<UiWelcomeIntroBinding>(R.layout.ui_welcome_intro) {
    override fun onResume() {
        super.onResume()
        activityViewModel.nextEnabled.postValue(true)
        activityViewModel.doneEnabled.postValue(false)
    }
}