package org.owntracks.android.ui.welcome

import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.ui.welcome.fragments.PlayFragmentViewModel

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {
  private val googleApiAvailability by lazy(NONE) { GoogleApiAvailability.getInstance() }
  private val playServicesRefresh = MutableStateFlow(0)
  private val shouldDisplayPlayServicesPage by
      lazy(NONE) {
        googleApiAvailability.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS
      }

  override fun additionalPages(snackbarHostState: SnackbarHostState): List<WelcomePageDescriptor> {
    if (!shouldDisplayPlayServicesPage) {
      return emptyList()
    }
    return listOf(
        WelcomePageDescriptor(
            key = "play_services",
            content = {
              PlayServicesPage(
                  welcomeViewModel = welcomeViewModel,
                  refreshFlow = playServicesRefresh.asStateFlow(),
                  onRequestFix = { requestPlayServicesResolution(snackbarHostState) },
                  onCheckAvailability = { playViewModel -> updatePlayServicesState(playViewModel) })
            }))
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
      notifyPlayServicesStateChanged()
    }
  }

  private fun requestPlayServicesResolution(snackbarHostState: SnackbarHostState) {
    val result = googleApiAvailability.isGooglePlayServicesAvailable(this)
    val shown =
        googleApiAvailability.showErrorDialogFragment(
            this, result, PLAY_SERVICES_RESOLUTION_REQUEST, null)
    if (!shown) {
      lifecycleScope.launch {
        snackbarHostState.showSnackbar(getString(R.string.play_services_not_available))
      }
    }
    notifyPlayServicesStateChanged()
  }

  private fun notifyPlayServicesStateChanged() {
    playServicesRefresh.update { it + 1 }
  }

  private fun updatePlayServicesState(
      playViewModel: PlayFragmentViewModel
  ): WelcomeViewModel.ProgressState {
    val result = googleApiAvailability.isGooglePlayServicesAvailable(this)
    return when (result) {
      ConnectionResult.SUCCESS -> {
        playViewModel.setPlayServicesAvailable(getString(R.string.play_services_now_available))
        WelcomeViewModel.ProgressState.PERMITTED
      }
      ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
      ConnectionResult.SERVICE_UPDATING -> {
        playViewModel.setPlayServicesNotAvailable(
            true, getString(R.string.play_services_update_required))
        WelcomeViewModel.ProgressState.NOT_PERMITTED
      }
      else -> {
        playViewModel.setPlayServicesNotAvailable(
            googleApiAvailability.isUserResolvableError(result),
            getString(R.string.play_services_not_available))
        WelcomeViewModel.ProgressState.NOT_PERMITTED
      }
    }
  }

  companion object {
    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 1
  }
}
