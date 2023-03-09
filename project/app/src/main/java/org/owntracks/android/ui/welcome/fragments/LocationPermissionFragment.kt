package org.owntracks.android.ui.welcome.fragments

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.databinding.UiWelcomeLocationPermissionBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.mixins.ActivityResultCallerWithLocationPermissionCallback
import org.owntracks.android.ui.mixins.LocationPermissionRequester
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class LocationPermissionFragment @Inject constructor() : WelcomeFragment(),
    ActivityResultCallerWithLocationPermissionCallback {
    private lateinit var binding: UiWelcomeLocationPermissionBinding

    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    @Inject
    lateinit var preferences: Preferences

    private val locationPermissionRequester = LocationPermissionRequester(this)

    override fun shouldBeDisplayed(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !requirementsChecker.isNotificationsEnabled()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UiWelcomeLocationPermissionBinding.inflate(inflater, container, false)
            .apply {
                uiFragmentWelcomeNotificationPermissionsRequest.setOnClickListener {
                    requestLocationPermissions()
                }
            }
        return binding.root
    }

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            preferences.userDeclinedEnableNotificationPermissions = !it
            if (it) {
                binding.uiFragmentWelcomeNotificationPermissionsRequest.visibility = View.INVISIBLE
                binding.uiFragmentWelcomeNotificationPermissionsMessage.visibility = View.VISIBLE
            }
            viewModel.setWelcomeState(WelcomeViewModel.ProgressState.PERMITTED)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestLocationPermissions() {
        notificationPermissionRequest.launch(POST_NOTIFICATIONS)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setWelcomeState(
            if (requirementsChecker.isNotificationsEnabled() || preferences.userDeclinedEnableNotificationPermissions) {
                WelcomeViewModel.ProgressState.PERMITTED
            } else {
                WelcomeViewModel.ProgressState.NOT_PERMITTED
            }
        )
    }

    override fun locationPermissionGranted(code: Int) {
        TODO("Not yet implemented")
    }

    override fun locationPermissionDenied(code: Int) {
        TODO("Not yet implemented")
    }
}
