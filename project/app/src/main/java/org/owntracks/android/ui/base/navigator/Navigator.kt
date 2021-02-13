package org.owntracks.android.ui.base.navigator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import org.owntracks.android.injection.scopes.PerActivity
import javax.inject.Inject

@PerActivity
class Navigator @Inject constructor(private val activity: AppCompatActivity) {
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
        activity.startActivityForResult(intent, requestCode)
    }

    companion object {
        private const val EXTRA_ARGS = "_args"
    }
}