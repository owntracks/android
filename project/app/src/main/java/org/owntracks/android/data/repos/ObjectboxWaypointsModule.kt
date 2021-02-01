package org.owntracks.android.data.repos

import dagger.Binds
import dagger.Module
import org.owntracks.android.data.repos.ObjectboxWaypointsRepo
import org.owntracks.android.data.repos.WaypointsRepo

@Module
abstract class ObjectboxWaypointsModule {
    @Binds
    abstract fun bindWaypointsRepo(objectboxWaypointsRepo: ObjectboxWaypointsRepo): WaypointsRepo
}