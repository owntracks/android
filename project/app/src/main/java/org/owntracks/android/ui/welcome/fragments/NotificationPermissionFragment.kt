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
import org.owntracks.android.databinding.UiWelcomeNotificationPermissionBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.welcome.WelcomeViewModel

@AndroidEntryPoint
class NotificationPermissionFragment @Inject constructor() : WelcomeFragment() {
  private lateinit var binding: UiWelcomeNotificationPermissionBinding

  @Inject lateinit var requirementsChecker: RequirementsChecker

  @Inject lateinit var preferences: Preferences

  override fun shouldBeDisplayed(context: Context): Boolean =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding =
        UiWelcomeNotificationPermissionBinding.inflate(inflater, container, false).apply {
          uiFragmentWelcomeNotificationPermissionsRequest.setOnClickListener {
            requestNotificationPermissions()
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
  private fun requestNotificationPermissions() {
    notificationPermissionRequest.launch(POST_NOTIFICATIONS)
  }

  override fun onResume() {
    super.onResume()
    viewModel.setWelcomeState(
        if (requirementsChecker.hasNotificationPermissions()) {
          binding.uiFragmentWelcomeNotificationPermissionsRequest.visibility = View.INVISIBLE
          binding.uiFragmentWelcomeNotificationPermissionsMessage.visibility = View.VISIBLE
          WelcomeViewModel.ProgressState.PERMITTED
        } else if (preferences.userDeclinedEnableNotificationPermissions) {
          WelcomeViewModel.ProgressState.PERMITTED
        } else {
          WelcomeViewModel.ProgressState.NOT_PERMITTED
        })
  }
}
