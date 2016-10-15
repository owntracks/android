package org.owntracks.android.ui.status;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiActivityContactsBinding;
import org.owntracks.android.databinding.UiActivityStatusBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.BaseActivity;

import timber.log.Timber;


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
