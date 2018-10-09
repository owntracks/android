package org.owntracks.android.ui.status;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiStatusBinding;
import org.owntracks.android.ui.base.BaseActivity;

import dagger.android.AndroidInjection;


public class StatusActivity extends BaseActivity<UiStatusBinding, StatusMvvm.ViewModel> implements StatusMvvm.View {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindAndAttachContentView(R.layout.ui_status, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);
    }
}
