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
import org.owntracks.android.ui.mixins.BackgroundLocationPermissionRequester
import org.owntracks.android.ui.mixins.LocationPermissionRequester
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class LocationPermissionFragment @Inject constructor() : WelcomeFragment() {
  private lateinit var binding: UiWelcomeLocationPermissionBinding

  @Inject lateinit var preferences: Preferences

  @Inject lateinit var requirementsChecker: RequirementsChecker

  private val locationPermissionRequester =
      LocationPermissionRequester(
          this,
          { preferences.userDeclinedEnableLocationPermissions = false },
          { preferences.userDeclinedEnableLocationPermissions = true })

  private val backgroundLocationPermissionRequester =
      BackgroundLocationPermissionRequester(
          this,
          { preferences.userDeclinedEnableBackgroundLocationPermissions = false },
          { preferences.userDeclinedEnableBackgroundLocationPermissions = true })

  override fun shouldBeDisplayed(context: Context): Boolean = true

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding =
        UiWelcomeLocationPermissionBinding.inflate(inflater, container, false).apply {
          uiFragmentWelcomeLocationPermissionsRequest.setOnClickListener {
            locationPermissionRequester.requestLocationPermissions(0, requireContext()) {
              shouldShowRequestPermissionRationale(it)
            }
          }
          uiFragmentWelcomeLocationBackgroundPermissionsRequest.setOnClickListener {
            backgroundLocationPermissionRequester.requestLocationPermissions(requireContext()) {
              false
            }
          }
        }
    return binding.root
  }

  override fun onResume() {
    super.onResume()
    viewModel.setWelcomeState(
        if (requirementsChecker.hasLocationPermissions()) {
          WelcomeViewModel.ProgressState.PERMITTED
        } else if (preferences.userDeclinedEnableLocationPermissions) {
          WelcomeViewModel.ProgressState.PERMITTED
        } else {
          WelcomeViewModel.ProgressState.NOT_PERMITTED
        })

    if (requirementsChecker.hasLocationPermissions() &&
        !requirementsChecker.hasBackgroundLocationPermission()) {
      binding.uiFragmentWelcomeLocationBackgroundPermissionsRequest.visibility = View.VISIBLE
      binding.uiFragmentWelcomeLocationPermissionsRequest.visibility = View.INVISIBLE
      binding.uiFragmentWelcomeLocationPermissionsMessage.visibility = View.INVISIBLE
    } else if (requirementsChecker.hasLocationPermissions() &&
        requirementsChecker.hasBackgroundLocationPermission()) {
      binding.uiFragmentWelcomeLocationBackgroundPermissionsRequest.visibility = View.INVISIBLE
      binding.uiFragmentWelcomeLocationPermissionsRequest.visibility = View.INVISIBLE
      binding.uiFragmentWelcomeLocationPermissionsMessage.visibility = View.VISIBLE
    } else {
      binding.uiFragmentWelcomeLocationBackgroundPermissionsRequest.visibility = View.INVISIBLE
      binding.uiFragmentWelcomeLocationPermissionsRequest.visibility = View.VISIBLE
      binding.uiFragmentWelcomeLocationPermissionsMessage.visibility = View.INVISIBLE
    }
  }

  private fun permissionDenied(@Suppress("UNUSED_PARAMETER") code: Int) {
    preferences.userDeclinedEnableLocationPermissions = true
  }
}
