package org.owntracks.android.ui.welcome;

import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.owntracks.android.injection.qualifier.ActivityFragmentManager;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.mode.ModeFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import java.util.ArrayList;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity
public class WelcomeAdapter extends FragmentStatePagerAdapter {

    private Resources res;
    private ArrayList<Integer> ids = new ArrayList<>();

    @Inject
    public WelcomeAdapter(@ActivityFragmentManager FragmentManager fm, Resources res) {
        super(fm);
        this.res = res;
    }

    public void addItemId(int id) {
        ids.add(id);
    }

    public int getLastItemId() {
        return ids.get(ids.size()-1);
    }

    public WelcomeFragmentMvvm.View getFragment(int position) {
        return WelcomeFragmentMvvm.View.class.cast(getItem(position));
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        Timber.v("position:%s id:%s", position, ids.get(position));

        switch (ids.get(position)) {
            case IntroFragment.ID:
                fragment = IntroFragment.getInstance();
                break;
            case ModeFragment.ID:
                fragment = ModeFragment.getInstance();
                break;
            case PlayFragment.ID:
                fragment = PlayFragment.getInstance();
                break;
            case PermissionFragment.ID:
                fragment = PermissionFragment.getInstance();
                break;
            case FinishFragment.ID:
                fragment = FinishFragment.getInstance();
                break;
            case VersionFragment.ID:
                fragment = VersionFragment.getInstance();
                break;

        }

        return fragment;
    }

    @Override
    public int getCount() {
        return ids.size();
    }
}