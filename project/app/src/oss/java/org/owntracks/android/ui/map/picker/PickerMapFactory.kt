package org.owntracks.android.ui.map.picker

import android.content.Context
import org.owntracks.android.preferences.Preferences

/** OpenStreetMap, as that's the only map there is */
fun pickerMapFor(context: Context, preferences: Preferences): PickerMap =
    OsmPickerMap(context, preferences)
