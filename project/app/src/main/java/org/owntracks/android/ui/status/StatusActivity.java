package org.owntracks.android.ui.status;

import android.os.Bundle;

import androidx.annotation.Nullable;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiStatusBinding;
import org.owntracks.android.ui.base.BaseActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StatusActivity extends BaseActivity<UiStatusBinding, StatusMvvm.ViewModel<StatusMvvm.View>> implements StatusMvvm.View {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindAndAttachContentView(R.layout.ui_status, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);
    }
}
