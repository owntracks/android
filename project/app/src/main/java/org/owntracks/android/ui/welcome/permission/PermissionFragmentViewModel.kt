package org.owntracks.android.ui.welcome.permission

import androidx.databinding.Bindable
import org.owntracks.android.BR
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@PerActivity
class PermissionFragmentViewModel @Inject internal constructor() : BaseViewModel<PermissionFragmentMvvm.View?>() {
    @get:Bindable
    var isPermissionGranted = false
        set(permissionGranted) {
            field = permissionGranted
            notifyPropertyChanged(BR.permissionGranted)
        }

    fun onFixClicked() {
        view!!.requestFix()
    }
}

