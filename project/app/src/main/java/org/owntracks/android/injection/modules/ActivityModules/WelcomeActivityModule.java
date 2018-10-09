package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import junit.runner.Version;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.modules.FragmentModules.FinishFragmentModule;
import org.owntracks.android.injection.modules.FragmentModules.IntroFragmentModule;
import org.owntracks.android.injection.modules.FragmentModules.PermissionFragmentModule;
import org.owntracks.android.injection.modules.FragmentModules.PlayFragmentModule;
import org.owntracks.android.injection.modules.FragmentModules.VersionFragmentModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.welcome.WelcomeActivity;
import org.owntracks.android.ui.welcome.WelcomeMvvm;
import org.owntracks.android.ui.welcome.WelcomeViewModel;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(includes = ActivityModule.class)
public abstract class WelcomeActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity appCompatActivity(WelcomeActivity a);

    @Binds
    @PerActivity
    abstract WelcomeMvvm.ViewModel bindViewModel(WelcomeViewModel viewModel);


    @ContributesAndroidInjector(modules = {PlayFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.welcome.play.PlayFragment bindPlayFragment();

    @ContributesAndroidInjector(modules = {IntroFragmentModule.class})
    abstract org.owntracks.android.ui.welcome.intro.IntroFragment bindIntroFragment();

    @ContributesAndroidInjector(modules = {VersionFragmentModule.class})
    abstract org.owntracks.android.ui.welcome.version.VersionFragment bindVersionFragment();

    @ContributesAndroidInjector(modules = {PermissionFragmentModule.class})
    abstract org.owntracks.android.ui.welcome.permission.PermissionFragment bindPermissionFragment();

    @ContributesAndroidInjector(modules = {FinishFragmentModule.class})
    abstract org.owntracks.android.ui.welcome.finish.FinishFragment bindFinishFragment();
}