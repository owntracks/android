package org.owntracks.android.ui.welcome.version

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeVersionBinding
import org.owntracks.android.ui.welcome.WelcomeViewModel
import javax.inject.Inject

@AndroidEntryPoint
class VersionFragment @Inject constructor() : Fragment() {
    private lateinit var binding: UiWelcomeVersionBinding
    private val activityViewModel: WelcomeViewModel by activityViewModels()
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.ui_welcome_version, container, false)
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