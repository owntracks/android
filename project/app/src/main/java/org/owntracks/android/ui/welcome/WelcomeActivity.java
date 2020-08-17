package org.owntracks.android.ui.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeBinding;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import javax.inject.Inject;

import timber.log.Timber;


public class WelcomeActivity extends BaseActivity<UiWelcomeBinding, WelcomeMvvm.ViewModel> implements WelcomeMvvm.View, ViewPager.OnPageChangeListener {
    @Inject
    WelcomeAdapter welcomeAdapter;

    private PlayFragment playFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(requirementsChecker.areRequirementsMet()) {
            navigator.startActivity(MapActivity.class, null, Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            finish();
            return;
        }

        bindAndAttachContentView(R.layout.ui_welcome, savedInstanceState);
        setHasEventBus(false);


        playFragment = new PlayFragment();
        welcomeAdapter.setupFragments(new IntroFragment(), new VersionFragment(), playFragment, new PermissionFragment(), new FinishFragment());

        binding.viewPager.setAdapter(welcomeAdapter);
        binding.viewPager.addOnPageChangeListener(this);



        Timber.v("pager setup with %s fragments", welcomeAdapter.getCount());
        buildPagerIndicator();
        showFragment(0);
    }

    @Override
    public void showNextFragment() {
        int currentItem = binding.viewPager.getCurrentItem();

        if(currentItem == welcomeAdapter.getLastItemPosition()) {
            Timber.e("viewPager is at the end");
            setNextEnabled(false);
            return;
        }
        showFragment(currentItem + 1);
    }

    public void setPagerIndicator(int index) {
        if (index < welcomeAdapter.getCount()) {
            for (int i = 0; i < welcomeAdapter.getCount(); i++) {
                ImageView circle = (ImageView) binding.circles.getChildAt(i);
                if (i == index) {
                    circle.setAlpha(1f);
                } else {
                    circle.setAlpha(0.5f);
                }
            }
        }
    }

    public void setNextEnabled(boolean enabled) {
        viewModel.setNextEnabled(enabled);
    }

    public void setDoneEnabled(boolean enabled) {
        viewModel.setDoneEnabled(enabled);
    }

    private void showPreviousFragment() {
        showFragment(binding.viewPager.getCurrentItem() - 1);
    }

    private void showFragment(int position) {
        binding.viewPager.setCurrentItem(position);
        welcomeAdapter.getFragment(binding.viewPager.getCurrentItem()).onShowFragment();
        refreshNextDoneButtons();
    }

    // TODO I really feel like we can replace this to auto refresh when the VM changes. Somehow.
    public void refreshNextDoneButtons() {
        setNextEnabled(welcomeAdapter.getFragment(binding.viewPager.getCurrentItem()).isNextEnabled());
        setDoneEnabled(binding.viewPager.getCurrentItem()  == welcomeAdapter.getLastItemPosition());
    }

    private void buildPagerIndicator() {
        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (5 * scale + 0.5f);

        for (int i = 0; i < welcomeAdapter.getCount(); i++) {
            ImageView circle = new ImageView(this);
            circle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_fiber_manual_record_24));
            circle.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            circle.setAdjustViewBounds(true);
            circle.setPadding(padding, 0, padding, 0);
            binding.circles.addView(circle);
        }
        setPagerIndicator(0);
    }

    @Override
    public void onBackPressed() {
        if (binding.viewPager.getCurrentItem() == 0) {
            finish();
        } else {
            showPreviousFragment();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        viewModel.onAdapterPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PlayFragment.PLAY_SERVICES_RESOLUTION_REQUEST) {
            playFragment.onPlayServicesResolutionResult();
        }
    }
}
