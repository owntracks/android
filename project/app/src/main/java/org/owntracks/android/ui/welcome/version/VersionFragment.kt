package org.owntracks.android.ui.welcome.version

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeVersionBinding
import org.owntracks.android.support.Events
import org.owntracks.android.ui.base.BaseSupportFragment
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm
import javax.inject.Inject

@AndroidEntryPoint
class VersionFragment @Inject constructor() :
    BaseSupportFragment<UiWelcomeVersionBinding?, NoOpViewModel?>(),
    WelcomeFragmentMvvm.View, View.OnClickListener {
    @Inject
    lateinit var eventBus: EventBus
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = setAndBindContentView(
            inflater,
            container,
            R.layout.ui_welcome_version,
            savedInstanceState
        )
        binding!!.uiFragmentWelcomeVersionButtonLearnMore.setOnClickListener(this)
        return v
    }

    override fun onClick(view: View) {
        try {
            val i =
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.documentationUrlAndroid)))
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                binding!!.root,
                getString(R.string.noBrowserInstalled),
                Snackbar.LENGTH_SHORT
            )
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(Events.WelcomeNextDoneButtonsEnableToggle())
    }
}