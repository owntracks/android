package org.owntracks.android.support

import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import timber.log.Timber

/**
 * This is a way to use a singleton to get a reference to the currently-running instance of a service. This gets injected
 * ints a service, which then calls `bind` during its onCreate
 *
 * @property runThingsOnOtherThreads
 * @constructor Create empty Service bridge
 */
@Singleton
class ServiceBridge @Inject internal constructor(private val runThingsOnOtherThreads: RunThingsOnOtherThreads) {
    private var serviceWeakReference: WeakReference<ServiceBridgeInterface?> = WeakReference(null)

    interface ServiceBridgeInterface {
        fun requestOnDemandLocationUpdate(reportType: MessageLocation.ReportType)
        fun sendEventNotification(message: MessageTransition)
    }

    fun bind(service: ServiceBridgeInterface) {
        Timber.d("Service bound to bridge")
        serviceWeakReference = WeakReference(service)
    }

    fun requestOnDemandLocationFix(reportType: MessageLocation.ReportType = MessageLocation.ReportType.RESPONSE) {
        serviceWeakReference.get()
            ?.apply {
                runThingsOnOtherThreads.postOnMainHandlerDelayed({
                    requestOnDemandLocationUpdate(reportType)
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
