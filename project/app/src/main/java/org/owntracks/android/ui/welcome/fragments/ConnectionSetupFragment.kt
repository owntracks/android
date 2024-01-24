package org.owntracks.android.ui.welcome.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeConnectionSetupBinding
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class ConnectionSetupFragment @Inject constructor() : WelcomeFragment() {
  override fun shouldBeDisplayed(context: Context): Boolean = true

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    return UiWelcomeConnectionSetupBinding.inflate(inflater, container, false)
        .apply {
          welcomeConnectionSetupLearnMoreButton.setOnClickListener {
            try {
              startActivity(
                  Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.documentationUrl))))
            } catch (e: ActivityNotFoundException) {
              Snackbar.make(root, getString(R.string.noBrowserInstalled), Snackbar.LENGTH_SHORT)
                  .show()
            }
          }
          screenDesc.movementMethod = ScrollingMovementMethod()
        }
        .root
  }

  override fun onResume() {
    super.onResume()
    viewModel.setWelcomeState(WelcomeViewModel.ProgressState.PERMITTED)
  }
}
