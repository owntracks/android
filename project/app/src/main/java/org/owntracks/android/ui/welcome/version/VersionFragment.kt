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
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeVersionBinding
import org.owntracks.android.ui.welcome.BaseWelcomeFragment
import javax.inject.Inject

@AndroidEntryPoint
class VersionFragment @Inject constructor() :
        BaseWelcomeFragment<UiWelcomeVersionBinding>(R.layout.ui_welcome_version) {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding.uiFragmentWelcomeVersionButtonLearnMore.setOnClickListener {
            try {
                val i =
                        Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.documentationUrlAndroid))
                        )
                startActivity(i)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                        binding.root,
                        R.string.welcomeVersionNoSuitableBrowserInstalled,
                        Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activityViewModel.nextEnabled.postValue(true)
        activityViewModel.doneEnabled.postValue(false)
    }
}