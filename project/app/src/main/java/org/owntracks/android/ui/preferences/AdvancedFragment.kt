package org.owntracks.android.ui.preferences

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.Binds
import dagger.Module
import org.owntracks.android.R
import org.owntracks.android.injection.modules.android.FragmentModules.BaseFragmentModule
import org.owntracks.android.injection.scopes.PerFragment
import org.owntracks.android.support.TimberLogFileTree
import timber.log.Timber

@PerFragment
class AdvancedFragment : AbstractPreferenceFragment() {
    companion object {
        const val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1311
    }

    var askedForPermission = false

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
        findPreference<Preference>(getString(R.string.preferenceKeyDebugLog))?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue: Any? ->
            if (newValue !is Boolean) false

            val askForPermission = { requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_STORAGE) }
            val hasExternalStoragePermission = { ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED }

            if (newValue as Boolean) { // User wants it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && askedForPermission) { // Supports shouldShowRequestPermissionRationale and User has previously asked for permission and said "no"
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) { // User didn't tell us to stop asking
                        AlertDialog
                                .Builder(context!!)
                                .setCancelable(true)
                                .setMessage(R.string.external_storage_permissions_description)
                                .setPositiveButton("OK") { _, _ -> askForPermission() }
                                .show()
                        false
                    } else { // User has denied and told us to stop bugging them.
                        false
                    }
                } else { // Either older device, or user has not denied before
                    if (!hasExternalStoragePermission()) {
                        askForPermission()
                        false
                    } else {
                        enableDebugLog()
                        true
                    }
                }
            } else { // User doesn't want it
                disableDebugLog()
                true
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                askedForPermission = false;
                enableDebugLog()
                (findPreference<Preference>(getString(R.string.preferenceKeyDebugLog)) as SwitchPreferenceCompat).isChecked = true
            } else {
                askedForPermission = true
                preferences.setDebugLog(false)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private fun enableDebugLog() {
        Timber.plant(TimberLogFileTree(activity))
        Timber.d("Debug logging enabled")
    }

    private fun disableDebugLog() {
        Timber.forest().filterIsInstance<TimberLogFileTree>().forEach { Timber.uproot(it) }
        Timber.i("Debug logging disabled")
    }

    @Module(includes = [BaseFragmentModule::class])
    internal abstract class AdvancedFragmentModule {
        @Binds
        @PerFragment
        abstract fun bindFragment(reportingFragment: AdvancedFragment): AdvancedFragment
    }

}
