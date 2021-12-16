package org.owntracks.android.ui.preferences.connection

import android.content.Context
import android.os.Bundle
import androidx.databinding.Bindable
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import org.greenrobot.eventbus.Subscribe
import org.owntracks.android.support.Events.ModeChanged
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.base.navigator.Navigator
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import org.owntracks.android.ui.preferences.connection.dialog.*
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class ConnectionViewModel @Inject internal constructor(
        private val preferences: Preferences,
        @param:ApplicationContext private val context: Context,
        private val navigator: Navigator
) : BaseViewModel<ConnectionMvvm.View>(), ConnectionMvvm.ViewModel<ConnectionMvvm.View> {
    private var modeId = 0
    override fun attachView(savedInstanceState: Bundle?, view: ConnectionMvvm.View) {
        super.attachView(savedInstanceState, view)
        setModeId(preferences.mode)
    }

    override fun setModeId(newModeId: Int) {
        modeId = newModeId
    }

    @Bindable
    override fun getModeId(): Int {
        return modeId
    }

    @Subscribe
    fun onEvent(e: ModeChanged) {
        Timber.v("mode changed %s", e.newModeId)
        setModeId(e.newModeId)
        view?.recreateOptionsMenu()
        notifyChange()
    }

    override fun onModeClick() {
        view?.showModeDialog()
    }

    override fun onHostClick() {
        view?.showHostDialog()
    }

    override fun onIdentificationClick() {
        view?.showIdentificationDialog()
    }

    override fun onSecurityClick() {
        view?.showSecurityDialog()
    }

    override fun onParametersClick() {
        view?.showParametersDialog()
    }

    override fun getModeDialogViewModel(): ConnectionModeDialogViewModel {
        return ConnectionModeDialogViewModel(preferences)
    }

    override fun getHostDialogViewModelMqtt(): ConnectionHostMqttDialogViewModel {
        return ConnectionHostMqttDialogViewModel(preferences)
    }

    override fun getHostDialogViewModelHttp(): ConnectionHostHttpDialogViewModel {
        return ConnectionHostHttpDialogViewModel(preferences)
    }

    override fun getIdentificationDialogViewModel(): ConnectionIdentificationViewModel {
        return ConnectionIdentificationViewModel(preferences)
    }

    override fun getConnectionSecurityViewModel(): ConnectionSecurityViewModel {
        return ConnectionSecurityViewModel(preferences, navigator, context)
    }

    override fun getConnectionParametersViewModel(): ConnectionParametersViewModel {
        return ConnectionParametersViewModel(preferences)
    }
}