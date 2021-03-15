package org.owntracks.android.ui.welcome.permission

import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@PerActivity
class PermissionFragmentViewModel @Inject internal constructor() : BaseViewModel<PermissionFragmentMvvm.View?>() {
    var isPermissionGranted = false
        set(permissionGranted) {
            field = permissionGranted
            notifyChange()
        }

    fun onFixClicked() {
        view!!.requestFix()
    }
}

