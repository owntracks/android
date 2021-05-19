package org.owntracks.android.ui.status

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.ui.base.BaseActivity

@AndroidEntryPoint
class StatusActivity : BaseActivity<UiStatusBinding?, StatusMvvm.ViewModel<StatusMvvm.View?>?>(),
    StatusMvvm.View {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindAndAttachContentView(R.layout.ui_status, savedInstanceState)
        setSupportToolbar(binding!!.appbar.toolbar)
        setDrawer(binding!!.appbar.toolbar)
    }
}