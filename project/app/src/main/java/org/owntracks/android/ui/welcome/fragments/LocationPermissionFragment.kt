package org.owntracks.android.ui.welcome.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.databinding.UiWelcomeLocationPermissionBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.mixins.LocationPermissionRequester
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class LocationPermissionFragment @Inject constructor() :
    WelcomeFragment() {
    private lateinit var binding: UiWelcomeLocationPermissionBinding

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    private val locationPermissionRequester = LocationPermissionRequester(this, ::permissionGranted, ::permissionDenied)

    override fun shouldBeDisplayed(context: Context): Boolean = !requirementsChecker.hasLocationPermissions()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UiWelcomeLocationPermissionBinding.inflate(inflater, container, false)
            .apply {
                uiFragmentWelcomeLocationPermissionsRequest.setOnClickListener {
                    requestLocationPermissions()
                }
            }
        return binding.root
    }

    private fun requestLocationPermissions() {
        locationPermissionRequester.requestLocationPermissions(
            0,
            requireContext()
        ) { shouldShowRequestPermissionRationale(it) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setWelcomeState(
            if (requirementsChecker.hasLocationPermissions() || preferences.userDeclinedEnableLocationPermissions) {
                WelcomeViewModel.ProgressState.PERMITTED
            } else {
                WelcomeViewModel.ProgressState.NOT_PERMITTED
            }
        )
    }

    private fun permissionGranted(code: Int) {
        preferences.userDeclinedEnableLocationPermissions = false
        binding.uiFragmentWelcomeLocationPermissionsRequest.visibility = View.INVISIBLE
        binding.uiFragmentWelcomeLocationPermissionsMessage.visibility = View.VISIBLE
        viewModel.setWelcomeState(WelcomeViewModel.ProgressState.PERMITTED)
    }

    private fun permissionDenied(code: Int) {
        preferences.userDeclinedEnableLocationPermissions = true
        viewModel.setWelcomeState(WelcomeViewModel.ProgressState.PERMITTED)
    }
}
