package org.owntracks.android.ui.map

import android.hardware.SensorEventListener
import android.location.Location
import androidx.lifecycle.LiveData
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel

interface MapMvvm {
    interface View : MvvmView {

    }
}