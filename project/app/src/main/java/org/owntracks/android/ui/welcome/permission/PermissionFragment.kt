package org.owntracks.android.ui.welcome.permission

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeIntroBinding
import org.owntracks.android.support.Events
import org.owntracks.android.ui.base.BaseSupportFragment
import javax.inject.Inject

class PermissionFragment @Inject constructor() :
    BaseSupportFragment<UiWelcomeIntroBinding?, PermissionFragmentViewModel>(),
    PermissionFragmentMvvm.View {
    @Inject
    lateinit var eventBus: EventBus
    private var askedForPermission = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return setAndBindContentView(
            inflater,
            container,
            R.layout.ui_welcome_permissions,
            savedInstanceState
        )
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            askedForPermission = true
            if (isGranted) {
                viewModel.isPermissionGranted = true
                eventBus.post(Events.WelcomeNextDoneButtonsEnableToggle())
                eventBus.postSticky(Events.PermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }

    override fun requestFix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (askedForPermission) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder(requireContext())
                        .setCancelable(true)
                        .setMessage(R.string.permissions_description)
                        .setPositiveButton(
                            "OK"
                        ) { _: DialogInterface?, _: Int -> requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                        .show()
                } else {
                    Toast.makeText(
                        this.context,
                        "Unable to proceed without location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkPermission() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    override fun onResume() {
        super.onResume()
        checkPermission().run {
            eventBus.post(Events.WelcomeNextDoneButtonsEnableToggle(this))
            viewModel.isPermissionGranted = this
        }
    }
}