package org.owntracks.android.ui.welcome

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomePlayBinding
import javax.inject.Inject

@AndroidEntryPoint
class PlayFragment @Inject constructor() : WelcomeFragment() {
    private val viewModel: WelcomeViewModel by activityViewModels()
    private val playFragmentViewModel: PlayFragmentViewModel by viewModels()
    private lateinit var binding: UiWelcomePlayBinding
    private val googleAPI = GoogleApiAvailability.getInstance()
    override fun shouldBeDisplayed(context: Context): Boolean =
        googleAPI.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = UiWelcomePlayBinding.inflate(inflater, container, false)
        binding.vm = playFragmentViewModel
        binding.lifecycleOwner = this.viewLifecycleOwner
        binding.recover.setOnClickListener {
            requestFix()
        }
        return binding.root
    }

    fun onPlayServicesResolutionResult() {
        checkGooglePlayservicesIsAvailable()
    }

    fun requestFix() {
        val result = googleAPI.isGooglePlayServicesAvailable(requireContext())

        if (!googleAPI.showErrorDialogFragment(
                requireActivity(),
                result,
                PLAY_SERVICES_RESOLUTION_REQUEST
            )
        ) {
            Snackbar.make(
                binding.root,
                getString(R.string.play_services_not_available),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        checkGooglePlayservicesIsAvailable()
    }

    private fun checkGooglePlayservicesIsAvailable() {
        val nextEnabled =
            when (val result = googleAPI.isGooglePlayServicesAvailable(requireContext())) {
                ConnectionResult.SUCCESS -> {
                    playFragmentViewModel.setPlayServicesAvailable(getString(R.string.play_services_now_available))
                    true
                }
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, ConnectionResult.SERVICE_UPDATING -> {
                    playFragmentViewModel.setPlayServicesNotAvailable(
                        true,
                        getString(R.string.play_services_update_required)
                    )
                    false
                }
                else -> {
                    playFragmentViewModel.setPlayServicesNotAvailable(
                        googleAPI.isUserResolvableError(
                            result
                        ), getString(R.string.play_services_not_available)
                    )
                    false
                }
            }
        if (nextEnabled) {
            viewModel.setWelcomeCanProceed()
        } else {
            viewModel.setWelcomeCannotProceed()
        }
    }

    override fun onResume() {
        super.onResume()

        checkGooglePlayservicesIsAvailable()
    }

    companion object {
        const val PLAY_SERVICES_RESOLUTION_REQUEST = 1
    }
}