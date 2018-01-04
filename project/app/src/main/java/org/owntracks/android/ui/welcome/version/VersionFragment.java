package org.owntracks.android.ui.welcome.version;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeVersionBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;

public class VersionFragment extends BaseFragment<UiWelcomeVersionBinding, NoOpViewModel> implements WelcomeFragmentMvvm.View, View.OnClickListener {
    public static final int ID = 5;

    private static VersionFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new VersionFragment();

        return instance;
    }

    public VersionFragment() {
        if(viewModel == null) { fragmentComponent().inject(this); }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this);};
        View v = setAndBindContentView(inflater, container, R.layout.ui_welcome_version, savedInstanceState);
        binding.uiFragmentWelcomeVersionButtonLearnMore.setOnClickListener(this);
        return v;

    }

    @Override
    public void onClick(View view) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(getString(R.string.valDocumentationUrlAndroid)));
        startActivity(i);
    }

    @Override
    public void onNextClicked() {

    }

    @Override
    public boolean isNextEnabled() {
        return true;
    }
}
