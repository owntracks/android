package org.owntracks.android.ui.status;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiStatusBinding;
import org.owntracks.android.ui.base.BaseActivity;


public class StatusActivity extends BaseActivity<UiStatusBinding, StatusMvvm.ViewModel> implements StatusMvvm.View {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_status, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);
    }
}
