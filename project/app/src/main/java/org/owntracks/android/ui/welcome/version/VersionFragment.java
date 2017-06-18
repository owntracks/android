package org.owntracks.android.ui.welcome.version;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiFragmentWelcomeVersionBinding;
import org.owntracks.android.ui.base.BaseFragment;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;

import timber.log.Timber;

public class VersionFragment extends BaseFragment<UiFragmentWelcomeVersionBinding, NoOpViewModel> implements WelcomeFragmentMvvm.View, View.OnClickListener {
    public static final int ID = 5;

    private static VersionFragment instance;
    public static Fragment getInstance() {
        if(instance == null)
            instance = new VersionFragment();
        return instance;
    }

    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";
    CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            CustomTabsClient mCustomTabsClient = client;
            if (mCustomTabsClient != null) {
                Timber.v("Starting warmup");
                mCustomTabsClient.warmup(0L);
                CustomTabsSession mCustomTabsSession = mCustomTabsClient.newSession(null);
                mCustomTabsSession.mayLaunchUrl(Uri.parse(getString(R.string.valDocumentationUrlAndroid)), null, null);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };




    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentComponent().inject(this);
        boolean ok = CustomTabsClient.bindCustomTabsService(getActivity(), CUSTOM_TAB_PACKAGE_NAME, connection);

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = setAndBindContentView(inflater, container, R.layout.ui_fragment_welcome_version, savedInstanceState);
        binding.uiFragmentWelcomeVersionButtonLearnMore.setOnClickListener(this);
        return v;

    }
    @Override
    public void onNextClicked() {

    }

    @Override
    public boolean canProceed() {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick(View view) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setToolbarColor(getResources().getColor(R.color.primary, getActivity().getTheme()));
        } else {
            builder.setToolbarColor(getResources().getColor(R.color.primary));
        }
        CustomTabsIntent customTabsIntent = builder.build();

        customTabsIntent.launchUrl(getActivity(), Uri.parse(getString(R.string.valDocumentationUrlAndroid)));
    }
}
