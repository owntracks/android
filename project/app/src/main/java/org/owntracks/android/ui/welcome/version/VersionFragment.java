package org.owntracks.android.ui.welcome.version;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeVersionBinding;
import org.owntracks.android.ui.base.BaseSupportFragment;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;

import java.time.Duration;

public class VersionFragment extends BaseSupportFragment<UiWelcomeVersionBinding, NoOpViewModel> implements WelcomeFragmentMvvm.View, View.OnClickListener {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = setAndBindContentView(inflater, container, R.layout.ui_welcome_version, savedInstanceState);
        binding.uiFragmentWelcomeVersionButtonLearnMore.setOnClickListener(this);
        return v;

    }

    @Override
    public void onClick(View view) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.valDocumentationUrlAndroid)));
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No suitable browser installed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNextClicked() {

    }

    @Override
    public boolean isNextEnabled() {
        return true;
    }

    @Override
    public void onShowFragment() {

    }
}
