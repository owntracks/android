package org.owntracks.android.injection.modules.android.ActivityModules;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.DrawerProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RequirementsChecker;
import org.owntracks.android.ui.base.navigator.Navigator;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class BaseActivityModule {
    public static final String ACTIVITY_FRAGMENT_MANAGER = "BaseActivityModule.activityFragmentManager";

    @Provides
    @PerActivity
    @ActivityContext
    static AppCompatActivity activityContext(AppCompatActivity activity) {
        return activity;
    }

    @Provides
    @PerActivity
    static DrawerProvider provideDrawerProvider(@ActivityContext AppCompatActivity context, Scheduler scheduler) {
        return new DrawerProvider(context, scheduler);
    }

    @Provides
    @PerActivity
    @Named(ACTIVITY_FRAGMENT_MANAGER)
    static FragmentManager provideFragmentManager(@ActivityContext AppCompatActivity context) {
        return context.getSupportFragmentManager();
    }


    @Provides
    @PerActivity
    static Navigator provideNavigator(@ActivityContext AppCompatActivity context) {
        return new Navigator(context);
    }

    @Provides
    @PerActivity
    static RequirementsChecker provideRequirementsChecker(@ActivityContext AppCompatActivity context, Preferences preferences) {
        return new RequirementsChecker(preferences, context);
    }
}
