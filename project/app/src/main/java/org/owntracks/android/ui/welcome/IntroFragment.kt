package org.owntracks.android.ui.welcome

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.databinding.UiWelcomeIntroBinding
import javax.inject.Inject

@AndroidEntryPoint
class IntroFragment @Inject constructor() : WelcomeFragment() {
    private val viewModel: WelcomeViewModel by activityViewModels()
    private lateinit var binding: UiWelcomeIntroBinding
    override fun shouldBeDisplayed(context: Context) = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = UiWelcomeIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.setWelcomeCanProceed()
    }
}
