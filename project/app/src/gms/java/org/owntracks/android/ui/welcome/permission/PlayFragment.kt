package org.owntracks.android.ui.welcome.permission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomePlayBinding
import org.owntracks.android.support.Events
import org.owntracks.android.ui.base.BaseSupportFragment
import javax.inject.Inject

@AndroidEntryPoint
class PlayFragment @Inject constructor() :
    BaseSupportFragment<UiWelcomePlayBinding?, PlayFragmentViewModel>(),
    PlayFragmentMvvm.View {
    @Inject
    lateinit var eventBus: EventBus
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return setAndBindContentView(
            inflater,
            container,
            R.layout.ui_welcome_play,
            savedInstanceState
        )
    }

    fun onPlayServicesResolutionResult() {
        checkAvailability()
    }

    override fun requestFix() {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(requireContext())
        if (!googleAPI.showErrorDialogFragment(
                requireActivity(),
                result,
                PLAY_SERVICES_RESOLUTION_REQUEST
            )
        ) {
            Toast.makeText(
                this.context,
                "Unable to update Google Play Services",
                Toast.LENGTH_SHORT
            ).show()
        }
        checkAvailability()
    }

    private fun checkAvailability() {
        val googleAPI = GoogleApiAvailability.getInstance()
        when (val result = googleAPI.isGooglePlayServicesAvailable(requireContext())) {
            ConnectionResult.SUCCESS -> {
                viewModel.fixAvailable = false
                viewModel.message = getString(R.string.play_services_now_available)
                true
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, ConnectionResult.SERVICE_UPDATING -> {
                viewModel.fixAvailable = true
                viewModel.message = getString(R.string.play_services_update_required)
                false
            }
            else -> {
                viewModel.fixAvailable = googleAPI.isUserResolvableError(result)
                viewModel.message = getString(R.string.play_services_not_available)
                false
            }
        }.run {
            eventBus.post(Events.WelcomeNextDoneButtonsEnableToggle(this))
        }
        binding?.invalidateAll()

    }

    override fun onResume() {
        super.onResume()
        checkAvailability()
    }

    companion object {
        const val PLAY_SERVICES_RESOLUTION_REQUEST = 1
    }
}