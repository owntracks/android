package org.owntracks.android.ui.status;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiActivityStatusBinding;
import org.owntracks.android.ui.base.BaseActivity;


public class StatusActivity extends BaseActivity<UiActivityStatusBinding, StatusMvvm.ViewModel> implements StatusMvvm.View {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setAndBindContentView(R.layout.ui_activity_status, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);
    }
}
