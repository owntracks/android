package org.owntracks.android.ui.welcome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiWelcomeBinding;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.map.MapActivity;

import javax.inject.Inject;

import timber.log.Timber;


public class WelcomeActivity extends BaseActivity<UiWelcomeBinding, WelcomeMvvm.ViewModel> implements WelcomeMvvm.View, ViewPager.OnPageChangeListener {
    @Inject
    WelcomeAdapter welcomeAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_welcome, savedInstanceState);
        setHasEventBus(false);
        if (requirementsChecker.areRequirementsMet(this)) {
            navigator.startActivity(MapActivity.class);
            finish();
        }

        welcomeAdapter.setupFragments();

        binding.viewPager.setAdapter(welcomeAdapter);
        binding.viewPager.addOnPageChangeListener(this);

        Timber.v("pager setup with %s fragments", welcomeAdapter.getCount());
        buildPagerIndicator();
        showFragment(0);

    }

    @Override
    public void showNextFragment() {
        int currentItem = binding.viewPager.getCurrentItem();
        welcomeAdapter.getFragment(currentItem).onNextClicked();
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

    @Override
    public void setNextEnabled(boolean enabled) {
        viewModel.setNextEnabled(enabled);
    }

    @Override
    public void setDoneEnabled(boolean enabled) {
        viewModel.setDoneEnabled(enabled);
    }

    public void showPreviousFragment() {
        showFragment(binding.viewPager.getCurrentItem() - 1);
    }

    public void showFragment(int position) {
        binding.viewPager.setCurrentItem(position);
        viewModel.setNextEnabled(welcomeAdapter.getFragment(position).isNextEnabled());
        viewModel.setDoneEnabled(position == welcomeAdapter.getLastItemPosition());
    }

    private void buildPagerIndicator() {
        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (5 * scale + 0.5f);

        for (int i = 0; i < welcomeAdapter.getCount(); i++) {
            ImageView circle = new ImageView(this);
            circle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fiber_manual_record_white_18dp));
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
}
