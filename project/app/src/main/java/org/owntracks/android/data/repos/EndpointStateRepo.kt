package org.owntracks.android.data.repos

import androidx.lifecycle.MutableLiveData
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.data.EndpointState
import timber.log.Timber

@Singleton
class EndpointStateRepo @Inject constructor() {
    var endpointState: EndpointState = EndpointState.INITIAL
        set(value) {
            field = value
            endpointStateLiveData.postValue(value)
        }

    fun setState(newEndpointState: EndpointState) {
        Timber.v(
            "Setting endpoint state $newEndpointState called from: ${Thread.currentThread().stackTrace[3].run {
                "$className: $methodName"
            }}"
        )
        endpointState = newEndpointState
    }

    fun setQueueLength(queueLength: Int) {
        Timber.v("Setting queuelength=$queueLength")
        endpointQueueLength.postValue(queueLength)
    }

    fun setServiceStartedNow() {
        serviceStartedDate.postValue(Date())
    }

    val endpointStateLiveData: MutableLiveData<EndpointState> =
        // TODO migrate this to Kotlin flow once we get AGP 7
        MutableLiveData(EndpointState.IDLE)

    val endpointQueueLength: MutableLiveData<Int> = MutableLiveData(0)

    val serviceStartedDate: MutableLiveData<Date> = MutableLiveData()
}
