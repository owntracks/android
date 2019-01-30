package org.owntracks.android.injection.modules.android.ActivityModules;

import androidx.appcompat.app.AppCompatActivity;


import org.owntracks.android.injection.modules.android.FragmentModules.FinishFragmentModule;
import org.owntracks.android.injection.modules.android.FragmentModules.IntroFragmentModule;
import org.owntracks.android.injection.modules.android.FragmentModules.PermissionFragmentModule;
import org.owntracks.android.injection.modules.android.FragmentModules.PlayFragmentModule;
import org.owntracks.android.injection.modules.android.FragmentModules.VersionFragmentModule;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.WelcomeActivity;
import org.owntracks.android.ui.welcome.WelcomeMvvm;
import org.owntracks.android.ui.welcome.WelcomeViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(includes = BaseActivityModule.class)
public abstract class WelcomeActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(WelcomeActivity a);

    @Binds
    @PerActivity
    abstract WelcomeMvvm.ViewModel bindViewModel(WelcomeViewModel viewModel);


    @ContributesAndroidInjector(modules = {PlayFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.welcome.play.PlayFragment bindPlayFragment();

    @ContributesAndroidInjector(modules = {IntroFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.welcome.intro.IntroFragment bindIntroFragment();

    @ContributesAndroidInjector(modules = {VersionFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.welcome.version.VersionFragment bindVersionFragment();

    @ContributesAndroidInjector(modules = {PermissionFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.welcome.permission.PermissionFragment bindPermissionFragment();

    @ContributesAndroidInjector(modules = {FinishFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.welcome.finish.FinishFragment bindFinishFragment();
}