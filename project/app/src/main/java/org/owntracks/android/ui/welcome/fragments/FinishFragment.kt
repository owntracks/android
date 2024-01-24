package org.owntracks.android.ui.welcome.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.databinding.UiWelcomeFinishBinding
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class FinishFragment @Inject constructor() : WelcomeFragment() {
  override fun shouldBeDisplayed(context: Context) = true

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    return UiWelcomeFinishBinding.inflate(inflater, container, false)
        .apply {
          uiFragmentWelcomeFinishOpenPreferences.setOnClickListener {
            viewModel.setWelcomeState(WelcomeViewModel.ProgressState.FINISHED)
            startActivity(
                Intent(requireContext(), PreferencesActivity::class.java).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
          }
        }
        .root
  }

  override fun onResume() {
    super.onResume()
    viewModel.setWelcomeState(WelcomeViewModel.ProgressState.FINISHED)
  }
}
