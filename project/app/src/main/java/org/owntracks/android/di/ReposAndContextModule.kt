package org.owntracks.android.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.App
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.MemoryContactsRepo
import org.owntracks.android.data.repos.ObjectboxWaypointsRepo
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.support.preferences.PreferencesStore
import org.owntracks.android.support.preferences.SharedPreferencesStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class ReposAndContextModule {
    @Binds
    @Singleton
    abstract fun provideContext(app: App): Context

    @Binds
    abstract fun bindSharedPreferencesStoreModule(sharedPreferencesStore: SharedPreferencesStore): PreferencesStore

    @Binds
    abstract fun bindWaypointsRepo(objectboxWaypointsRepo: ObjectboxWaypointsRepo): WaypointsRepo

    @Binds
    abstract fun bindMemoryContactsRepo(memoryContactsRepo: MemoryContactsRepo): ContactsRepo
}