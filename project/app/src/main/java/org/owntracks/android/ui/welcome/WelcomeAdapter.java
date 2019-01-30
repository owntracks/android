package org.owntracks.android.ui.welcome;

import android.os.Build;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.owntracks.android.injection.modules.android.ActivityModules.BaseActivityModule;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.support.RequirementsChecker;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

@PerActivity
public class WelcomeAdapter extends FragmentStatePagerAdapter  {


    private final ArrayList<Fragment> fragments = new ArrayList<>();

    private RequirementsChecker requirementsChecker;

    @Inject
    WelcomeAdapter(@Named(BaseActivityModule.ACTIVITY_FRAGMENT_MANAGER) FragmentManager fm, RequirementsChecker requirementsChecker) {
        super(fm);
        this.requirementsChecker = requirementsChecker;

    }

    public void setupFragments(Fragment introFragment, Fragment versionFragment, Fragment playFragment, Fragment permissionFragment, Fragment finishFragment ) {
        fragments.add(introFragment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            fragments.add(versionFragment);


        if (!requirementsChecker.isPlayCheckPassed()) {
            fragments.add(playFragment);
        }

        if (!requirementsChecker.isPermissionCheckPassed()) {
            fragments.add(permissionFragment);
        }
        fragments.add(finishFragment);

    }

    public int getLastItemPosition() {
        return fragments.size() - 1;
    }


    public WelcomeFragmentMvvm.View getFragment(int position) {
        return WelcomeFragmentMvvm.View.class.cast(getItem(position));
    }

    @Override
    public Fragment getItem(final int position) {
        if (position >= fragments.size()) {
            Timber.e("Welcome position %d is out of bounds for fragment list length %d", position, fragments.size());
            throw new IndexOutOfBoundsException();
        }
        Timber.v("position:%s fragment:%s", position, fragments.get(position).toString());
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}