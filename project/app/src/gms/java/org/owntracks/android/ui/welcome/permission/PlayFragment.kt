package org.owntracks.android.ui.welcome.permission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomePlayBinding
import org.owntracks.android.ui.welcome.WelcomeViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PlayFragment @Inject constructor() : Fragment() {
    private val viewModel: PlayFragmentViewModel by viewModels()
    private val activityViewModel: WelcomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: UiWelcomePlayBinding =
            DataBindingUtil.inflate(inflater, R.layout.ui_welcome_play, container, false)
        binding.vm = viewModel
        binding.lifecycleOwner = this

        binding.recover.setOnClickListener {
            val googleAPI = GoogleApiAvailability.getInstance()
            val result = googleAPI.isGooglePlayServicesAvailable(requireContext())
            if (!googleAPI.showErrorDialogFragment(
                    requireActivity(),
                    result,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                )
            ) {
                Snackbar.make(
                    binding.root,
                    R.string.welcomePlayUnableToUpdateGooglePlayServices,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            checkAvailability()
        }
        return binding.root
    }

    fun onPlayServicesResolutionResult() {
        checkAvailability()
    }

    private fun checkAvailability() {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(requireContext())
        activityViewModel.nextEnabled.postValue(false)
        when (result) {
            ConnectionResult.SUCCESS -> {
                viewModel.message.postValue(getString(R.string.play_services_now_available))
                activityViewModel.nextEnabled.postValue(true)
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, ConnectionResult.SERVICE_UPDATING -> {
                viewModel.message.postValue(getString(R.string.play_services_update_required))
            }
            ConnectionResult.SERVICE_DISABLED -> {
                viewModel.message.postValue(getString(R.string.play_services_disabled))
            }
            else -> {
                viewModel.message.postValue(getString(R.string.play_services_not_available))
            }
        }
        viewModel.fixAvailable.postValue(googleAPI.isUserResolvableError(result))
    }

    override fun onResume() {
        super.onResume()
        activityViewModel.doneEnabled.postValue(false)
        checkAvailability()
    }

    companion object {
        const val PLAY_SERVICES_RESOLUTION_REQUEST = 1
    }
}