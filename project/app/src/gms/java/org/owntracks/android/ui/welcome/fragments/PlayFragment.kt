package org.owntracks.android.ui.welcome.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomePlayBinding
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class PlayFragment @Inject constructor() : WelcomeFragment() {
  private val playFragmentViewModel: PlayFragmentViewModel by viewModels()
  private lateinit var binding: UiWelcomePlayBinding
  private val googleAPI = GoogleApiAvailability.getInstance()

  override fun shouldBeDisplayed(context: Context): Boolean =
      googleAPI.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding =
        UiWelcomePlayBinding.inflate(inflater, container, false).apply {
          playFragmentViewModel.playServicesFixAvailable.observe(this@PlayFragment) { available ->
            recover.isEnabled = available
          }
          recover.setOnClickListener { requestFix() }
        }
    return binding.root
  }

  fun onPlayServicesResolutionResult() {
    checkGooglePlayservicesIsAvailable()
  }

  private fun requestFix() {
    val result = googleAPI.isGooglePlayServicesAvailable(requireContext())

    if (!googleAPI.showErrorDialogFragment(
        requireActivity(),
        result,
        PLAY_SERVICES_RESOLUTION_REQUEST,
    )) {
      Snackbar.make(
              binding.root,
              getString(R.string.play_services_not_available),
              Snackbar.LENGTH_SHORT,
          )
          .show()
    }
    checkGooglePlayservicesIsAvailable()
  }

  private fun checkGooglePlayservicesIsAvailable() {
    val nextEnabled =
        when (val result = googleAPI.isGooglePlayServicesAvailable(requireContext())) {
          ConnectionResult.SUCCESS -> {
            playFragmentViewModel.setPlayServicesAvailable(
                getString(R.string.play_services_now_available),
            )
            WelcomeViewModel.ProgressState.PERMITTED
          }

          ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
          ConnectionResult.SERVICE_UPDATING -> {
            playFragmentViewModel.setPlayServicesNotAvailable(
                true,
                getString(R.string.play_services_update_required),
            )
            WelcomeViewModel.ProgressState.NOT_PERMITTED
          }

          else -> {
            playFragmentViewModel.setPlayServicesNotAvailable(
                googleAPI.isUserResolvableError(result),
                getString(R.string.play_services_not_available),
            )
            WelcomeViewModel.ProgressState.NOT_PERMITTED
          }
        }
    viewModel.setWelcomeState(nextEnabled)
  }

  override fun onResume() {
    super.onResume()
    checkGooglePlayservicesIsAvailable()
  }

  companion object {
    const val PLAY_SERVICES_RESOLUTION_REQUEST = 1
  }
}
