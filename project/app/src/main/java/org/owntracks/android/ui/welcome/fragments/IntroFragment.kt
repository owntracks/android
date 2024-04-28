package org.owntracks.android.ui.welcome.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.databinding.UiWelcomeIntroBinding
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class IntroFragment @Inject constructor() : WelcomeFragment() {
  override fun shouldBeDisplayed(context: Context) = true

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    return UiWelcomeIntroBinding.inflate(inflater, container, false).root
  }

  override fun onResume() {
    super.onResume()
    viewModel.setWelcomeState(WelcomeViewModel.ProgressState.PERMITTED)
  }
}
