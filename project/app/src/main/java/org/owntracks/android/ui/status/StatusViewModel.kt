package org.owntracks.android.ui.status

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.PowerManager
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import org.greenrobot.eventbus.Subscribe
import org.owntracks.android.BR
import org.owntracks.android.services.MessageProcessor.EndpointState
import org.owntracks.android.support.Events.QueueChanged
import org.owntracks.android.support.Events.ServiceStarted
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import org.owntracks.android.ui.status.logs.LogViewerActivity
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ActivityScoped
class StatusViewModel @Inject constructor(@param:ApplicationContext private val context: Context) :
        BaseViewModel<StatusMvvm.View>(), StatusMvvm.ViewModel<StatusMvvm.View> {
    private var endpointState: EndpointState? = null
    private var endpointMessage: String? = null
    private var serviceStarted: Date? = null
    private var locationUpdated: Long = 0
    private var queueLength = 0
    private val dozeWhitelisted: MutableLiveData<Boolean> = MutableLiveData()


    @Bindable
    override fun getEndpointState(): EndpointState {
        return endpointState ?: EndpointState.IDLE
    }

    @Bindable
    override fun getEndpointMessage(): String {
        return endpointMessage ?: ""
    }

    @Bindable
    override fun getEndpointQueue(): Int {
        return queueLength
    }

    @Bindable
    override fun getServiceStarted(): Date? {
        return serviceStarted
    }

    override fun getDozeWhitelisted(): LiveData<Boolean> = dozeWhitelisted

    override fun refreshDozeModeWhitelisted() {
        dozeWhitelisted.postValue(isIgnoringBatteryOptimizations())
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                (context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                        context.applicationContext.packageName
                )
    }

    @Bindable
    override fun getLocationUpdated(): Long {
        return locationUpdated
    }

    @Subscribe(sticky = true)
    fun onEvent(e: EndpointState) {
        endpointState = e
        endpointMessage = e.message
        notifyPropertyChanged(BR.endpointState)
        notifyPropertyChanged(BR.endpointMessage)
    }

    @Subscribe(sticky = true)
    fun onEvent(e: ServiceStarted) {
        serviceStarted = e.date
        notifyPropertyChanged(BR.serviceStarted)
    }

    @Subscribe(sticky = true)
    fun onEvent(l: Location) {
        locationUpdated = TimeUnit.MILLISECONDS.toSeconds(l.time)
        notifyPropertyChanged(BR.locationUpdated)
    }

    @Subscribe(sticky = true)
    fun onEvent(e: QueueChanged) {
        Timber.v("queue changed %s", e.newLength)
        queueLength = e.newLength
        notifyPropertyChanged(BR.endpointQueue)
    }

    fun viewLogs() {
        val intent =
                Intent(context, LogViewerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}