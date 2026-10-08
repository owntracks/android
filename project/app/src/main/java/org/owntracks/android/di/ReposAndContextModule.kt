package org.owntracks.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.MemoryContactsRepo
import org.owntracks.android.data.waypoints.RoomWaypointsRepo
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.location.profiles.AndroidDeviceContextProvider
import org.owntracks.android.location.profiles.DeviceContextProvider
import org.owntracks.android.preferences.PreferencesStore
import org.owntracks.android.preferences.SharedPreferencesStore

@InstallIn(SingletonComponent::class)
@Module
abstract class ReposAndContextModule {
  @Binds
  abstract fun bindSharedPreferencesStoreModule(
      sharedPreferencesStore: SharedPreferencesStore
  ): PreferencesStore

  @Binds abstract fun bindWaypointsRepo(waypointsRepo: RoomWaypointsRepo): WaypointsRepo

  @Binds
  abstract fun bindDeviceContextProvider(
      androidDeviceContextProvider: AndroidDeviceContextProvider
  ): DeviceContextProvider

  @Binds abstract fun bindMemoryContactsRepo(memoryContactsRepo: MemoryContactsRepo): ContactsRepo
}
