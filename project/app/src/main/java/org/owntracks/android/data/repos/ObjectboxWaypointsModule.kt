package org.owntracks.android.data.repos

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class ObjectboxWaypointsModule {
    @Binds
    abstract fun bindWaypointsRepo(objectboxWaypointsRepo: ObjectboxWaypointsRepo): WaypointsRepo
}

