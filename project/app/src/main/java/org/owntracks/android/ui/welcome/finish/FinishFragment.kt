package org.owntracks.android.ui.welcome.finish

import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeFinishBinding
import org.owntracks.android.ui.welcome.BaseWelcomeFragment
import javax.inject.Inject

@AndroidEntryPoint
class FinishFragment @Inject constructor() :
        BaseWelcomeFragment<UiWelcomeFinishBinding>(R.layout.ui_welcome_finish) {

    override fun onResume() {
        super.onResume()
        activityViewModel.nextEnabled.postValue(false)
        activityViewModel.doneEnabled.postValue(true)
    }
}