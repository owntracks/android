package org.owntracks.android.geocoding

import android.content.Context
import android.content.SharedPreferences
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.R
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.injection.scopes.PerApplication
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.preferences.OnModeChangedPreferenceChangedListener
import timber.log.Timber
import javax.inject.Inject

@PerApplication
class GeocoderProvider @Inject constructor(@AppContext val context: Context, val preferences: Preferences) {
    private lateinit var geocoder: Geocoder

    private fun setGeocoderProvider(@AppContext context: Context, preferences: Preferences) {
        Timber.i("Setting geocoding provider to ${preferences.reverseGeocodeProvider}")
        geocoder = when (preferences.reverseGeocodeProvider) {
            Preferences.REVERSE_GEOCODE_PROVIDER_OPENCAGE -> OpenCageGeocoder(preferences.openCageGeocoderApiKey)
            Preferences.REVERSE_GEOCODE_PROVIDER_GOOGLE -> GoogleGeocoder(context)
            else -> GeocoderNone()
        }
    }

    private suspend fun geocoderResolve(messageLocation: MessageLocation): String? {
        return withContext(Dispatchers.IO) {
            return@withContext geocoder.reverse(messageLocation.latitude, messageLocation.longitude)
        }
    }

    fun resolve(messageLocation: MessageLocation, backgroundService: BackgroundService) {
        if (!messageLocation.hasGeocoder()) {
            backgroundService.onGeocodingProviderResult(messageLocation)
            return
        }
        GlobalScope.launch {
            val result = geocoderResolve(messageLocation)
            messageLocation.geocode=result
            backgroundService.onGeocodingProviderResult(messageLocation)
        }
    }

    fun resolve(messageLocation: MessageLocation, textView: TextView) {
        if (!messageLocation.hasGeocoder()) {
            textView.text = messageLocation.geocode
            return
        }
        textView.text = messageLocation.fallbackGeocode // will print lat : lon until GeocodingProvider is available
        GlobalScope.launch {
            val result = geocoderResolve(messageLocation)
            messageLocation.geocode=result
            textView.text = messageLocation.geocode
        }
    }

    init {
        setGeocoderProvider(context, preferences)
        preferences.registerOnPreferenceChangedListener(object : OnModeChangedPreferenceChangedListener {
            override fun onAttachAfterModeChanged() {

            }

            override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
                if (key == preferences.getPreferenceKey(R.string.preferenceKeyReverseGeocodeProvider) || key == preferences.getPreferenceKey(R.string.preferenceKeyOpencageGeocoderApiKey)) {
                    setGeocoderProvider(context, preferences)
                }
            }
        })
    }
}