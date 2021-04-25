package org.owntracks.android.ui.map

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.api.ResolvableApiException

class LocationLifecycleObserver(private val registry: ActivityResultRegistry) : DefaultLifecycleObserver {
    lateinit var resultLauncher: ActivityResultLauncher<IntentSenderRequest>
    lateinit var callback: (Boolean) -> Unit
    override fun onCreate(owner: LifecycleOwner) {
        resultLauncher = registry.register("key", owner, ActivityResultContracts.StartIntentSenderForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> callback(true)
                else -> callback(false)
            }
        }
    }

    fun resolveException(exception: ResolvableApiException, callback: (Boolean) -> Unit) {
        this.callback = callback
        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
        resultLauncher.launch(intentSenderRequest)
    }
}