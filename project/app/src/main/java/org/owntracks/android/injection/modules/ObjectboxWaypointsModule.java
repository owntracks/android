package org.owntracks.android.injection.modules;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.repos.ObjectboxWaypointsRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.support.Preferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ObjectboxWaypointsModule {
    @Provides
    @Singleton
    protected WaypointsRepo provideWaypointsRepo(@AppContext Context context, EventBus eventBus, Preferences preferences) {
        return new ObjectboxWaypointsRepo(context, eventBus, preferences);
    }
}
