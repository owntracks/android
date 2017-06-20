package org.owntracks.android.ui.welcome;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiActivityWelcomeBinding;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.mode.ModeFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import javax.inject.Inject;

import timber.log.Timber;


public class WelcomeActivity extends BaseActivity<UiActivityWelcomeBinding, WelcomeMvvm.ViewModel> implements WelcomeMvvm.View, ViewPager.OnPageChangeListener {
    @Inject
    WelcomeAdapter viewPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_activity_welcome, savedInstanceState);
        setHasEventBus(false);
        setupPagerAdapter();
    }

    private void setupPagerAdapter() {
        requirementsChecker.assertRequirements(this);

        if(!requirementsChecker.isInitialSetupCheckPassed()) {
            viewPagerAdapter.addItemId(IntroFragment.ID);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                viewPagerAdapter.addItemId(VersionFragment.ID);

            viewPagerAdapter.addItemId(ModeFragment.ID);
        }

        if(!requirementsChecker.isPlayCheckPassed()) {
            viewPagerAdapter.addItemId(PlayFragment.ID);
        }

        if(!requirementsChecker.isPermissionCheckPassed()) {
            viewPagerAdapter.addItemId(PermissionFragment.ID);
        }

        viewPagerAdapter.addItemId(FinishFragment.ID);


        binding.viewPager.setAdapter(viewPagerAdapter);
        binding.viewPager.addOnPageChangeListener(this);

        Timber.v("pager setup with %s fragments", viewPagerAdapter.getCount());
        buildPagerIndicator();
        setPagerIndicator(0);
        viewModel.setFragmentViewModel(WelcomeFragmentMvvm.View.class.cast(getCurrentFragment()).getViewModel());


        //binding.setFragmentVm(WelcomeFragmentMvvm.View.class.cast(getCurrentFragment()).getViewModel());


    }

    @Override
    public void onBackPressed() {
        if (binding.viewPager.getCurrentItem() == 0) {
            finish();
        } else {
            binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() - 1);
        }
    }

    private void buildPagerIndicator() {
        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (5 * scale + 0.5f);

        for (int i = 0; i < viewPagerAdapter.getCount(); i++) {
            ImageView circle = new ImageView(this);
            circle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fiber_manual_record_white_18dp));
            circle.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            circle.setAdjustViewBounds(true);
            circle.setPadding(padding, 0, padding, 0);
            binding.circles.addView(circle);
        }

    }

    public void setPagerIndicator(int index) {
        if (index < viewPagerAdapter.getCount()) {
            for (int i = 0; i < viewPagerAdapter.getCount(); i++) {
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
    public void setFragmentViewModel(WelcomeFragmentMvvm.ViewModel fragmentViewModel) {
        this.viewModel.setFragmentViewModel(fragmentViewModel);
    }

    @Override
    public void showNextFragment() {
        WelcomeFragmentMvvm.View.class.cast(getCurrentFragment()).getViewModel().onNextClicked();
        binding.viewPager.forward();
        viewModel.setFragmentViewModel(WelcomeFragmentMvvm.View.class.cast(getCurrentFragment()).getViewModel());
    }

    public WelcomeFragmentMvvm.View getCurrentFragment() {
        return viewPagerAdapter.getFragment(binding.viewPager.getCurrentItem());
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
        viewModel.onAdapterPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {}
}
