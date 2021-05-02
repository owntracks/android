package org.owntracks.android.support

import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceBridge @Inject internal constructor() {
    private var serviceWeakReference: WeakReference<ServiceBridgeInterface?> = WeakReference(null)

    interface ServiceBridgeInterface {
        fun requestOnDemandLocationUpdate()
    }

    fun bind(service: ServiceBridgeInterface) {
        serviceWeakReference = WeakReference(service)
    }

    fun requestOnDemandLocationFix() {
        if (serviceWeakReference.get() == null) {
            Timber.e("missing service reference")
            return
        }
        serviceWeakReference.get()?.requestOnDemandLocationUpdate()
    }
}