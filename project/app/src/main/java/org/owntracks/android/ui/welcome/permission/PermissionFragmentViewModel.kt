package org.owntracks.android.ui.welcome.permission

import androidx.databinding.Bindable
import dagger.hilt.android.scopes.ActivityScoped
import org.owntracks.android.BR
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@ActivityScoped
class PermissionFragmentViewModel @Inject internal constructor() :
    BaseViewModel<PermissionFragmentMvvm.View?>() {
    @get:Bindable
    var isPermissionGranted = false
        set(permissionGranted) {
            field = permissionGranted
            notifyPropertyChanged(BR.permissionGranted)
        }

    @get:Bindable
    var isPermissionRequired = false
        set(permissionRequired) {
            field = permissionRequired
            notifyPropertyChanged(BR.permissionRequired)
        }

    fun onFixClicked() {
        view!!.requestFix()
    }
}

