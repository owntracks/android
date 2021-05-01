package org.owntracks.android.data.repos

import dagger.Binds
import dagger.Module

@Module
abstract class ObjectboxWaypointsModule {
    @Binds
    abstract fun bindWaypointsRepo(objectboxWaypointsRepo: ObjectboxWaypointsRepo): WaypointsRepo
}