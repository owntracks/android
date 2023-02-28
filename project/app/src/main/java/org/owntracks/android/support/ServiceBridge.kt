package org.owntracks.android.support

import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.model.messages.MessageTransition
import timber.log.Timber

@Singleton
class ServiceBridge @Inject internal constructor(private val runThingsOnOtherThreads: RunThingsOnOtherThreads) {
    private var serviceWeakReference: WeakReference<ServiceBridgeInterface?> = WeakReference(null)

    interface ServiceBridgeInterface {
        fun requestOnDemandLocationUpdate()
        fun sendEventNotification(message: MessageTransition)
    }

    fun bind(service: ServiceBridgeInterface) {
        serviceWeakReference = WeakReference(service)
    }

    fun requestOnDemandLocationFix() {
        serviceWeakReference.get()
            ?.apply {
                runThingsOnOtherThreads.postOnMainHandlerDelayed({
                    requestOnDemandLocationUpdate()
                })
            }
            ?.run { Timber.e("missing service reference") }
    }

    fun sendEventNotification(message: MessageTransition) {
        serviceWeakReference.get()
            ?.apply {
                runThingsOnOtherThreads.postOnMainHandlerDelayed({
                    sendEventNotification(message)
                })
            }
            ?.run { Timber.e("missing service reference") }
    }
}
