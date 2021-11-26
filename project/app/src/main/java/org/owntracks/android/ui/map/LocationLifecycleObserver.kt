package org.owntracks.android.ui.map

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class LocationLifecycleObserver(private val registry: ActivityResultRegistry) : DefaultLifecycleObserver {
    private lateinit var resultLauncher: ActivityResultLauncher<IntentSenderRequest>
    var callback: (Boolean) -> Unit = {
        Timber.w("Location lifecycleobserver callback has not been set. Noop.")
    }

    override fun onCreate(owner: LifecycleOwner) {
        resultLauncher = registry.register("key", owner, ActivityResultContracts.StartIntentSenderForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> callback(true)
                else -> callback(false)
            }
        }
    }
}