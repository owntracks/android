package org.owntracks.android.ui.welcome.version;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomeVersionBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.welcome.WelcomeMvvm;

import timber.log.Timber;

public class VersionFragment extends BaseFragment<UiFragmentWelcomeVersionBinding, VersionFragmentMvvm.ViewModel> implements VersionFragmentMvvm.View, View.OnClickListener {
    public static final int ID = 5;

    private static VersionFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new VersionFragment();
        return instance;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(viewModel == null) { fragmentComponent().inject(this); }
        View v = setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_version, savedInstanceState);
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
    public VersionFragmentMvvm.ViewModel getViewModel() {
        return viewModel;
    }
    @Override
    public void setActivityViewModel() {
        WelcomeMvvm.View.class.cast(getActivity()).setFragmentViewModel(viewModel);
    }
}
