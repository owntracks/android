package org.owntracks.android.ui.exit;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesBinding;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;

// This class isn't used for much, it's only used as a class to pass around. It should never
// be started.
public class ExitActivity extends BaseActivity<UiPreferencesBinding, NoOpViewModel> implements MvvmView {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
    }
}
