package org.owntracks.android.ui.welcome.play

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomePlayBinding
import org.owntracks.android.ui.base.BaseSupportFragment
import org.owntracks.android.ui.welcome.WelcomeMvvm

class PlayFragment : BaseSupportFragment<UiWelcomePlayBinding?, PlayFragmentViewModel>(), PlayFragmentMvvm.View {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_play, savedInstanceState)
    }

    fun onPlayServicesResolutionResult() {
        checkAvailability()
    }

    override fun requestFix() {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(requireContext())
        if (!googleAPI.showErrorDialogFragment(requireActivity(), result, PLAY_SERVICES_RESOLUTION_REQUEST)) {
            Toast.makeText(this.context, "Unable to update Google Play Services", Toast.LENGTH_SHORT).show()
        }
        checkAvailability()
    }

    private var canProceed = false
    private fun checkAvailability() {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(requireContext())
        var fixAvailable = false
        val playServicesStatusMessage: String
        when (result) {
            ConnectionResult.SUCCESS -> {
                canProceed = true
                playServicesStatusMessage = getString(R.string.play_services_now_available)
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, ConnectionResult.SERVICE_UPDATING -> {
                canProceed = false
                fixAvailable = true
                playServicesStatusMessage = getString(R.string.play_services_update_required)
            }
            else -> {
                canProceed = false
                fixAvailable = googleAPI.isUserResolvableError(result)
                playServicesStatusMessage = getString(R.string.play_services_not_available)
            }
        }
        viewModel.fixAvailable= fixAvailable
        viewModel.message = playServicesStatusMessage
        (activity as WelcomeMvvm.View?)!!.refreshNextDoneButtons()
        binding!!.invalidateAll()
    }

    override fun isNextEnabled(): Boolean {
        return canProceed
    }

    override fun onShowFragment() {
        checkAvailability()
    }

    companion object {
        const val PLAY_SERVICES_RESOLUTION_REQUEST = 1
    }
}