package org.owntracks.android.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.support.ContactImageBindingAdapter

/**
 * Hilt entry point for providing dependencies to Compose screens.
 * Used when screens are hosted in NavHost and need access to dependencies
 * that would normally be injected into Activities.
 */
@EntryPoint
@InstallIn(ActivityComponent::class)
interface ComposablesEntryPoint {
    fun contactImageBindingAdapter(): ContactImageBindingAdapter
    fun preferences(): Preferences
    fun messageProcessor(): MessageProcessor
}
