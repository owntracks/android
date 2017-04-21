package org.owntracks.android.ui.configuration;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiActivityConfigurationBinding;
import org.owntracks.android.ui.base.BaseActivity;

import timber.log.Timber;

public class ConfigurationActivity extends BaseActivity<UiActivityConfigurationBinding, ConfigurationMvvm.ViewModel> implements ConfigurationMvvm.View {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.v("onCreate");
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setAndBindContentView(R.layout.ui_activity_configuration, savedInstanceState);

        setHasEventBus(false);
        setSupportToolbar(binding.toolbar);
    }


    @Override
    public void displayLoadError() {

    }
}
