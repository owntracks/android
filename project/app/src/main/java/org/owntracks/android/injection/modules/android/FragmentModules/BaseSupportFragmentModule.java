package org.owntracks.android.injection.modules.android.FragmentModules;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.owntracks.android.injection.qualifier.DefaultFragmentManager;
import org.owntracks.android.injection.scopes.PerFragment;

import javax.inject.Named;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public abstract class BaseSupportFragmentModule {
    public static final String FRAGMENT = "BaseSupportFragmentModule.fragment";
    public static final String CHILD_FRAGMENT_MANAGER = "BaseSupportFragmentModule.childFragmentManager";

    @Binds
    @PerFragment
    abstract Fragment bindSupportFragment(Fragment f);

    @Provides
    @PerFragment
    @DefaultFragmentManager
    static FragmentManager provideDefaultFragmentManager(Fragment f) { return f.getFragmentManager(); }

    @Provides
    @Named(CHILD_FRAGMENT_MANAGER)
    @PerFragment
    static FragmentManager childFragmentManager(@Named(FRAGMENT) Fragment fragment) {
        return fragment.getChildFragmentManager();
    }
}
