package org.owntracks.android.injection.modules;

import android.app.FragmentManager;
import org.owntracks.android.injection.qualifier.DefaultFragmentManager;
import org.owntracks.android.injection.scopes.PerFragment;

import javax.inject.Named;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public abstract class BaseFragmentModule {

    public static final String FRAGMENT = "BaseFragmentModule.fragment";
    public static final String CHILD_FRAGMENT_MANAGER = "BaseFragmentModule.childFragmentManager";

    @Binds
    @PerFragment
    abstract android.app.Fragment bindFragment(android.app.Fragment f);

    @Provides
    @PerFragment
    @DefaultFragmentManager
    static FragmentManager provideDefaultFragmentManager(android.app.Fragment f) { return f.getFragmentManager(); }

    @Provides
    @Named(CHILD_FRAGMENT_MANAGER)
    @PerFragment
    static android.app.FragmentManager childFragmentManager(@Named(FRAGMENT) android.app.Fragment fragment) {
        return fragment.getChildFragmentManager();
    }
}
