package org.owntracks.android.ui.welcome.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeIntroBinding
import org.owntracks.android.ui.welcome.WelcomeViewModel
import javax.inject.Inject

@AndroidEntryPoint
class IntroFragment @Inject constructor() : Fragment() {
    private val activityViewModel: WelcomeViewModel by activityViewModels()
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val binding: UiWelcomeIntroBinding =
                DataBindingUtil.inflate(inflater, R.layout.ui_welcome_intro, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activityViewModel.nextEnabled.postValue(true)
        activityViewModel.doneEnabled.postValue(false)
    }
}