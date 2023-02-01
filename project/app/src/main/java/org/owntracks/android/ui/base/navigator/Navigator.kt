package org.owntracks.android.ui.base.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class Navigator @Inject constructor(@ActivityContext private val activity: Context) {

    fun startActivityForResult(intent: Intent?, requestCode: Int) {
        (activity as Activity).startActivityForResult(intent, requestCode)
    }

    companion object {
        private const val EXTRA_ARGS = "_args"
    }
}
