package org.owntracks.android.ui.welcome;

import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.owntracks.android.injection.qualifier.ActivityFragmentManager;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.support.RequirementsChecker;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import java.util.ArrayList;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity
public class WelcomeAdapter extends FragmentStatePagerAdapter {

    @Inject IntroFragment introFragment;

    @Inject VersionFragment versionFragment;

    @Inject PlayFragment playFragment;

    @Inject PermissionFragment permissionFragment;

    @Inject FinishFragment finishFragment;

    private final ArrayList<Fragment> fragments = new ArrayList<>();

    private RequirementsChecker requirementsChecker;

    @Inject
    WelcomeAdapter(@ActivityFragmentManager FragmentManager fm, RequirementsChecker requirementsChecker) {
        super(fm);
        this.requirementsChecker = requirementsChecker;
    }

    public void setupFragments() {
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
        if (fragments.size() < position) {
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