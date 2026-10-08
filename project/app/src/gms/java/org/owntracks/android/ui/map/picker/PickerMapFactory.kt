package org.owntracks.android.ui.map.picker

import android.content.Context
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.ui.map.GoogleMapFragment

/** The same kind of map as the user has chosen for the main map */
fun pickerMapFor(context: Context, preferences: Preferences): PickerMap =
    preferences.mapLayerStyle.let {
      if (it.getFragmentClass() == GoogleMapFragment::class.java) {
        GooglePickerMap(context, it)
      } else {
        OsmPickerMap(context, preferences)
      }
    }
