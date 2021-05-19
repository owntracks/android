package org.owntracks.android.ui.welcome.finish

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeFinishBinding
import org.owntracks.android.support.Events
import org.owntracks.android.ui.base.BaseSupportFragment
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm
import javax.inject.Inject

class FinishFragment @Inject constructor() : BaseSupportFragment<UiWelcomeFinishBinding?, NoOpViewModel?>(),
    WelcomeFragmentMvvm.View {
    @Inject
    lateinit var eventBus: EventBus
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return setAndBindContentView(
            inflater,
            container,
            R.layout.ui_welcome_finish,
            savedInstanceState
        )
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(
            Events.WelcomeNextDoneButtonsEnableToggle(
                nextEnabled = false,
                doneEnabled = true
            )
        )
    }
}