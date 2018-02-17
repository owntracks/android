package org.owntracks.android.injection.modules;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;

import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.qualifier.ChildFragmentManager;
import org.owntracks.android.injection.qualifier.DefaultFragmentManager;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.base.navigator.FragmentNavigator;
import org.owntracks.android.ui.base.navigator.SupportFragmentNavigator;
import org.owntracks.android.ui.base.navigator.Navigator;

import dagger.Module;
import dagger.Provides;

@Module
public class FragmentModule {

    private final Fragment mFragment;

    public FragmentModule(Fragment fragment) {
        mFragment = fragment;
    }

    @Provides
    @PerFragment
    @ActivityContext
    Context provideActivityContext() { return mFragment.getActivity(); }

    @Provides
    @PerFragment
    @DefaultFragmentManager
    FragmentManager provideDefaultFragmentManager() { return mFragment.getFragmentManager(); }

    @Provides
    @PerFragment
    @ChildFragmentManager
    FragmentManager provideChildFragmentManager() { return mFragment.getChildFragmentManager(); }

    @Provides
    @PerFragment
    Navigator provideNavigator() { return new FragmentNavigator(mFragment); }

}
