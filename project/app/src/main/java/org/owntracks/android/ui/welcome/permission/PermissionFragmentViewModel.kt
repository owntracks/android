package org.owntracks.android.ui.welcome.permission

import android.os.Bundle
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

    override fun attachView(savedInstanceState: Bundle?, view: PermissionFragmentMvvm.View) {
        super.attachView(savedInstanceState, view)
    }

    fun onFixClicked() {
        view!!.requestFix()
    }
}

