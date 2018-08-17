package org.owntracks.android.injection.modules;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.qualifier.ChildFragmentManager;
import org.owntracks.android.injection.qualifier.DefaultFragmentManager;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.base.navigator.SupportFragmentNavigator;

import dagger.Module;
import dagger.Provides;

@Module
public class SupportFragmentModule {

    private final android.support.v4.app.Fragment mFragment;

    public SupportFragmentModule(android.support.v4.app.Fragment fragment) {
        mFragment = fragment;
    }

    @Provides
    @PerActivity
    @ActivityContext
    Context provideActivityContext() { return mFragment.getActivity(); }

    @Provides
    @PerActivity
    @DefaultFragmentManager
    FragmentManager provideDefaultFragmentManager() { return mFragment.getFragmentManager(); }

    @Provides
    @PerActivity
    @ChildFragmentManager
    FragmentManager provideChildFragmentManager() { return mFragment.getChildFragmentManager(); }

    @Provides
    @PerActivity
    Navigator provideNavigator() { return new SupportFragmentNavigator(mFragment); }

}
