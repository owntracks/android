package org.owntracks.android.ui.welcome.version

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeVersionBinding
import org.owntracks.android.ui.base.BaseSupportFragment
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm

class VersionFragment : BaseSupportFragment<UiWelcomeVersionBinding?, NoOpViewModel?>(), WelcomeFragmentMvvm.View, View.OnClickListener {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = setAndBindContentView(inflater, container, R.layout.ui_welcome_version, savedInstanceState)
        binding!!.uiFragmentWelcomeVersionButtonLearnMore.setOnClickListener(this)
        return v
    }

    override fun onClick(view: View) {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.documentationUrlAndroid)))
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No suitable browser installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun isNextEnabled(): Boolean {
        return true
    }

    override fun onShowFragment() {}
}