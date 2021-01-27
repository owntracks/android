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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomePermissionsBinding
import org.owntracks.android.support.Events.PermissionGranted
import org.owntracks.android.ui.base.BaseSupportFragment
import org.owntracks.android.ui.welcome.WelcomeMvvm
import javax.inject.Inject

class PermissionFragment : BaseSupportFragment<UiWelcomePermissionsBinding?, PermissionFragmentViewModel>(), PermissionFragmentMvvm.View {
    private var askedForPermission = false

    @Inject
    lateinit var eventBus: EventBus

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return setAndBindContentView(inflater, container, R.layout.ui_welcome_permissions, savedInstanceState)
    }

    override fun requestFix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (askedForPermission) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder(requireContext())
                            .setCancelable(true)
                            .setMessage(R.string.permissions_description)
                            .setPositiveButton("OK"
                            ) { _: DialogInterface?, _: Int -> requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Companion.PERMISSIONS_REQUEST_CODE) }
                            .show()
                } else {
                    Toast.makeText(this.context, "Unable to proceed without location permissions.", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Companion.PERMISSIONS_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Companion.PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        askedForPermission = true
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            eventBus.postSticky(PermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION))
        }
        (activity as WelcomeMvvm.View?)!!.refreshNextDoneButtons()
    }

    private fun checkPermission() {
        if (context != null) {
            viewModel!!.isPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun isNextEnabled(): Boolean {
        checkPermission()
        return if (viewModel != null) {
            viewModel!!.isPermissionGranted
        } else {
            false
        }
    }

    override fun onShowFragment() {}

    companion object {
        private  const val PERMISSIONS_REQUEST_CODE = 1
    }
}