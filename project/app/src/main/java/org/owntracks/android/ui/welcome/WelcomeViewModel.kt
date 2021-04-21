package org.owntracks.android.ui.welcome

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.support.Events
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber
import javax.inject.Inject

@PerActivity
class WelcomeViewModel @Inject constructor(private val preferences: Preferences, private val eventBus: EventBus) : BaseViewModel<WelcomeMvvm.View?>() {
    var currentFragmentPosition: MutableLiveData<Int> = MutableLiveData(0)
    var doneEnabled = false
        set(value) {
            field = value
            notifyChange()
        }
    var nextEnabled = false
        set(value) {
            field = value
            notifyChange()
        }

    fun moveNext() {
        currentFragmentPosition.postValue(currentFragmentPosition.value!! + 1)
        notifyChange()
    }

    fun moveBack() {
        currentFragmentPosition.postValue(currentFragmentPosition.value!! - 1)
        notifyChange()
    }

    fun onDoneClicked() {
        Timber.v("onDoneClicked next:%s, done:%s", nextEnabled, doneEnabled)
        preferences.setSetupCompleted()
        navigator.startActivity(MapActivity::class.java, null, Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(e: Events.WelcomeNextDoneButtonsEnableToggle) {
        nextEnabled = e.nextEnabled
        doneEnabled = e.doneEnabled
    }

    // The eventbus gets unregistered onPause, so we have to wire up onResume so that events... work?
    fun onResume() {
        eventBus.register(this)
    }
}