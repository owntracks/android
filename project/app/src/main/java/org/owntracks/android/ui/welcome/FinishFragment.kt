package org.owntracks.android.ui.welcome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.databinding.UiWelcomeFinishBinding
import org.owntracks.android.ui.preferences.PreferencesActivity
import javax.inject.Inject

@AndroidEntryPoint
class FinishFragment @Inject constructor() : WelcomeFragment() {
    private val viewModel: WelcomeViewModel by activityViewModels()
    private lateinit var binding: UiWelcomeFinishBinding
    override fun shouldBeDisplayed(context: Context) = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = UiWelcomeFinishBinding.inflate(inflater, container, false)
        binding.uiFragmentWelcomeFinishOpenPreferences.setOnClickListener {
            viewModel.setWelcomeIsAtEnd()
            startActivity(Intent(requireContext(), PreferencesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.setWelcomeIsAtEnd()
    }
}