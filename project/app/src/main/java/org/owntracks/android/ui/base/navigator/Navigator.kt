package org.owntracks.android.ui.base.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class Navigator @Inject constructor(@ActivityContext private val activity: Context) {
    fun startActivity(intent: Intent) {
        activity.startActivity(intent)
    }

    fun startActivity(action: String) {
        activity.startActivity(Intent(action))
    }

    fun startActivity(action: String, uri: Uri) {
        activity.startActivity(Intent(action, uri))
    }

    fun startActivity(activityClass: Class<out Activity?>) {
        startActivity(activityClass, null, 0)
    }

    fun startActivity(activityClass: Class<out Activity?>, args: Bundle?, flags: Int) {
        val activity = activity
        val intent = Intent(activity, activityClass)
        intent.flags = flags
        if (args != null) {
            intent.putExtra(EXTRA_ARGS, args)
        }
        activity.startActivity(intent)
    }

    fun startActivity(activityClass: Class<out Activity?>, args: Parcelable?) {
        val activity = activity
        val intent = Intent(activity, activityClass)
        if (args != null) {
            intent.putExtra(EXTRA_ARGS, args)
        }
        activity.startActivity(intent)
    }

    fun getExtrasBundle(intent: Intent): Bundle? {
        return if (intent.hasExtra(EXTRA_ARGS)) intent.getBundleExtra(EXTRA_ARGS) else Bundle()
    }

    fun startActivityForResult(intent: Intent?, requestCode: Int) {
        (activity as Activity).startActivityForResult(intent, requestCode)
    }

    companion object {
        private const val EXTRA_ARGS = "_args"
    }
}
