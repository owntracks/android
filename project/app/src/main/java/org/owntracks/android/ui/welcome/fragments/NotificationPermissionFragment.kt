package org.owntracks.android.ui.welcome.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.databinding.UiWelcomeNotificationPermissionBinding

@AndroidEntryPoint
class NotificationPermissionFragment @Inject constructor() : WelcomeFragment() {

    override fun shouldBeDisplayed(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return UiWelcomeNotificationPermissionBinding.inflate(inflater, container, false).root
    }

    override fun onResume() {
        super.onResume()
        viewModel.setWelcomeCanProceed()
    }
}
