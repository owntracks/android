package org.owntracks.android.robolectric;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.support.Preferences;

import dagger.Module;
import dagger.Provides;

@Module
public class DummyWaypointsModule{
    @PerApplication
    @Provides
    protected WaypointsRepo provideWaypointsRepo(@AppContext Context context, EventBus eventBus, Preferences preferences) {
        return new DummyWaypointsRepo(eventBus);
    }

}
